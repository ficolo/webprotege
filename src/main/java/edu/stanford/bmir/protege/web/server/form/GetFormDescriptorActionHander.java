package edu.stanford.bmir.protege.web.server.form;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.collection.CollectionElementDataRepository;
import edu.stanford.bmir.protege.web.server.dispatch.AbstractHasProjectActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.hierarchy.AssertedClassHierarchyProvider;
import edu.stanford.bmir.protege.web.shared.collection.CollectionElementData;
import edu.stanford.bmir.protege.web.shared.collection.CollectionElementId;
import edu.stanford.bmir.protege.web.shared.collection.CollectionId;
import edu.stanford.bmir.protege.web.shared.form.*;
import edu.stanford.bmir.protege.web.shared.form.data.FormDataValue;
import edu.stanford.bmir.protege.web.shared.form.field.*;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 07/04/16
 */
public class GetFormDescriptorActionHander extends AbstractHasProjectActionHandler<GetFormDescriptorAction, GetFormDescriptorResult> {

    private final ProjectId projectId;

    private final AssertedClassHierarchyProvider classHierarchyProvider;

    private final OWLDataFactory dataFactory;

    private final CollectionElementDataRepository repository;

    private final CollectionId dummyId = CollectionId.get("12345678-1234-1234-1234-123456789abc");

    @Inject
    public GetFormDescriptorActionHander(@Nonnull AccessManager accessManager,
                                         ProjectId projectId,
                                         AssertedClassHierarchyProvider classHierarchyProvider,
                                         OWLDataFactory dataFactory,
                                         CollectionElementDataRepository repository) {
        super(accessManager);
        this.projectId = projectId;
        this.classHierarchyProvider = classHierarchyProvider;
        this.dataFactory = dataFactory;
        this.repository = repository;
    }


    @Override
    public Class<GetFormDescriptorAction> getActionClass() {
        return GetFormDescriptorAction.class;
    }

    public GetFormDescriptorResult execute(GetFormDescriptorAction action, ExecutionContext executionContext) {
        return getDummy(action.getFormId(),
                        action.getSubject());
    }


    private GetFormDescriptorResult getDummy(FormId formId, OWLEntity entity) {
        try {
            if (!entity.isOWLClass()) {
                return new GetFormDescriptorResult(projectId, entity, FormDescriptor.empty(), FormData.empty());
            }

            URL url = GetFormDescriptorActionHander.class.getResource("/amino-acid-form.json");
            System.out.println(url);
            InputStream is = GetFormDescriptorActionHander.class.getResourceAsStream("/amino-acid-form.json");

            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(FormDataValue.class, new FormDataValueDeserializer(dataFactory));
            module.addSerializer(new EntitySerializer());
            module.addSerializer(new IRISerializer());
            module.addSerializer(new LiteralSerializer());
            module.addSerializer(new FormElementIdSerializer());
            module.addSerializer(new FormDataSerializer());
            module.addSerializer(new FormDataObjectSerializer());
            mapper.registerModule(module);
            mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
            FormDescriptor d = mapper.readerFor(FormDescriptor.class).readValue(new BufferedInputStream(is));

            is.close();

            CollectionElementId id = CollectionElementId.get(entity.toStringID());
            CollectionElementData formData = repository.find(dummyId, id);
            return new GetFormDescriptorResult(
                    projectId,
                    entity,
                    d,
                    formData.getFormData().orElse(FormData.empty())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
