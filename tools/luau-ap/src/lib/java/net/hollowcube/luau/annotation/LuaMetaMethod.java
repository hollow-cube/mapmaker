package net.hollowcube.luau.annotation;

public @interface LuaMetaMethod {

    //__index -> Control 'prototype' inheritance. When accessing "myTable[key]" and the key does not appear in the table, but the metatable has an __index property:
    //__newindex -> Control property assignment. When calling "myTable[key] = value", if the metatable has a __newindex key pointing to a function, call that function, passing it the table, key, and value.
    //__mode -> Control weak references. A string value with one or both of the characters 'k' and 'v' which specifies that the the keys and/or values in the table are weak references.
    //__call -> Treat a table like a function. When a table is followed by parenthesis such as "myTable( 'foo' )" and the metatable has a __call key pointing to a function, that function is invoked (passing the table as the first argument, followed by any specified arguments) and the return value is returned.
    //__namecall -> Faster alternative to obj:Method(args). Only invoked on userdata and builtin types.
    //__tostring -> Control string representation. When the builtin "tostring( myTable )" function is called, if the metatable for myTable has a __tostring property set to a function, that function is invoked (passing myTable to it) and the return value is used as the string representation.
    //__len -> Control table length that is reported. When the table length is requested using the length operator ( '#' ), if the metatable for myTable has a __len key pointing to a function, that function is invoked (passing myTable to it) and the return value used as the value of "#myTable".
    //__iter -> todo
    //__type -> String field describing the type name of the object
    //__metatable -> Hide the metatable. When "getmetatable( myTable )" is called, if the metatable for myTable has a __metatable key, the value of that key is returned instead of the actual metatable.

    //__unm -> Unary minus. When writing "-myTable", if the metatable has a __unm key pointing to a function, that function is invoked (passing the table), and the return value used as the value of "-myTable".
    // __add -> Addition. When writing "myTable + object" or "object + myTable", if myTable's metatable has an __add key pointing to a function, that function is invoked (passing the left and right operands in order) and the return value used.
    //__sub -> Subtraction. Invoked similar to addition, using the '-' operator.
    //__mul -> Multiplication. Invoked similar to addition, using the '*' operator.
    //__div -> Division. Invoked similar to addition, using the '/' operator.
    //__idiv -> Floor division (division with rounding down to nearest integer). '//' operator.
    //__mod -> Modulo. Invoked similar to addition, using the '%' operator.
    //__pow -> Involution. Invoked similar to addition, using the '^' operator.
    //__concat -> Concatenation. Invoked similar to addition, using the '..' operator.

    //__eq -> Check for equality. This method is invoked when "myTable1 == myTable2" is evaluated, but only if both tables have the exact same metamethod for __eq.
    //__le -> Check for less-than. Similar to equality, using the '<' operator.
    //__lt -> Check for less-than-or-equal. Similar to equality, using the '<=' operator.

}
