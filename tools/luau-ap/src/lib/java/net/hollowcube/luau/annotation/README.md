# Annotation Based Luau Bindings

This module provides a set of annotations, as well as an annotation processor. The processor generates the necessary
glue logic to bind Java types to a Lua state at compile time. There are three major goals of this module:

1. API users should not need a deep understanding of the Lua C API.
2. The Java types should be written as idiomatic Java code.
3. *Most* Luau type definitions and documentation should be generated. And any handwritten types should be defined
   with the relevant type.

# `@LuaObject`

TODO

# `@LuaProperty`

TODO

# `@LuaMethod`

TODO

# Pins

A pin is a way to hold a reference to the lua value of a `@LuaObject`. Typically, pins are used to avoid
repeatedly allocating and deallocating lua userdata objects for the same java object instance.

Pinned objects may opt to include a cleanup method by implementing the `Pinned` interface. The `unpin` method will
be called when the reference is being released.

Pinned objects at the root must be released manually. A `@LuaProperty` of a `Pin` will be automatically released when
the owning object is released. For example, given the following scenario:

```java
import net.hollowcube.luau.net.hollowcube.luau.util.Pin;

@LuaObject
public class MyObject {
    @LuaProperty
    public final Pin<SomethingElse> somethingElse;
}

// Pin<MyObject> objPin = Pin.value(new MyObject());
// objPin.close(); // This will release the somethingElse pin as well
```

TODO: Can we have some auto release mechanism? Eg use the lua gc flag to release the java hold? It would require that
we only hold a weak reference to the Lua object. We could probably do it with a weak table, but it may just not be
worth it.
The whole core issue is that both Java and Lua must agree on when an object is no longer needed, but that is circular,
so it just seems impossible to do safely.

