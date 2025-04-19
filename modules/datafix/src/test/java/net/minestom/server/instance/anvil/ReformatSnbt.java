package net.minestom.server.instance.anvil;

import net.kyori.adventure.nbt.TagStringIOExt;

public class ReformatSnbt {
    public static void main(String[] args) throws Exception {
        var snbt = """
                
                {CustomName:{text:"@"},Command:"/time set 1000",x:36,y:65,z:218,id:"minecraft:command_block",SuccessCount:1,LastOutput:{extra:[{with:["1000"],translate:"commands.time.set"}],text:"[20:53:42] "},TrackOutput:1b}
                
                
                
                
                
                
                """;

        System.out.println(TagStringIOExt.writeTag(TagStringIOExt.readTag(snbt), "    "));
    }
}
