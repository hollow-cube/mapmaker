# Blueprint format

| Name                    | Type         | Notes                        |
|-------------------------|--------------|------------------------------|
| Version                 | byte         |                              |
| Size                    | Vec3         |                              |
| Offset                  | Vec3         |                              |
| Length of block palette | short        | Number of elements following |
| Block palette           | array(block) |                              |
| Block data              | []long       | Compressed palette data      |

# Block

| Name            | Type    | Notes                                                            |
|-----------------|---------|------------------------------------------------------------------|
| Block           | String  | ID and properties                                                |
| Block entity id | String  | Empty string indicates                                           |
| Has block data  | Boolean | Whether there is block data, next element only present when true |
| Block data      | NBT     |                                                                  |
