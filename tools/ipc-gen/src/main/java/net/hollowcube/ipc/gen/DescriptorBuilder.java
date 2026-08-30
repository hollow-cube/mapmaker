package net.hollowcube.ipc.gen;

import net.hollowcube.ipc.gen.wire.WireDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/// Turns what the processor found — the services and the types the walker reached from them — into
/// the [WireDescriptor] written to `wire.json`.
final class DescriptorBuilder {

    static WireDescriptor build(List<IpcModel> models, WireWalker walker,
                                SortedMap<String, String> subjects, SortedMap<String, String> notifications) {
        var services = new TreeMap<String, WireDescriptor.Service>();
        for (var model : models) {
            var methods = new TreeMap<String, WireDescriptor.Method>();
            for (var method : model.methods()) {
                var params = new ArrayList<WireDescriptor.Field>();
                for (var parameter : method.parameters()) {
                    params.add(new WireDescriptor.Field(parameter.getSimpleName().toString(),
                        WireTypeName.render(parameter.asType()), Nullability.isNullable(parameter, parameter.asType())));
                }
                var returns = method.isVoid() ? null : new WireDescriptor.Slot(WireTypeName.render(method.returnType()),
                    Nullability.isNullable(method.element(), method.returnType()));
                methods.put(method.route(), new WireDescriptor.Method(params, returns));
            }
            services.put(model.serviceName(), new WireDescriptor.Service(model.interfaceName().canonicalName(), methods));
        }

        var types = new TreeMap<String, WireDescriptor.Type>();
        for (var record : walker.records()) {
            var fields = new ArrayList<WireDescriptor.Field>();
            for (var field : record.fields()) {
                fields.add(new WireDescriptor.Field(field.name(), WireTypeName.render(field.type()), field.nullable()));
            }
            var typeParameters = record.element().getTypeParameters().stream()
                .map(parameter -> parameter.getSimpleName().toString())
                .toList();
            types.put(record.element().getQualifiedName().toString(), WireDescriptor.Type.record(typeParameters, fields, record.uses()));
        }
        for (var wireEnum : walker.enums()) {
            types.put(wireEnum.element().getQualifiedName().toString(), WireDescriptor.Type.enumeration(wireEnum.constants()));
        }
        for (var sealed : walker.sealeds()) {
            var variants = new TreeMap<String, String>();
            sealed.variants().forEach((name, variant) -> variants.put(name, variant.getQualifiedName().toString()));
            types.put(sealed.element().getQualifiedName().toString(), WireDescriptor.Type.sealed(IpcNames.DISCRIMINATOR, variants));
        }

        return new WireDescriptor(services, types, subjects, notifications);
    }

    private DescriptorBuilder() {
    }
}
