# Replay binary format

| Name         | Type      | Notes                             |
|--------------|-----------|-----------------------------------|
| Version      | VarInt    |                                   |
| User Version | VarInt    |                                   |
| Tick Rate    | VarInt    | Ticks per second of the recording |
| Length       | VarInt    | Length, in ticks                  |
| Origin       | Point     | Origin position of the recording  |
| Tick Data    | see below | Repeated for each tick            |

# Tick Data (repeated for each tick)

| Name              | Type      | Notes |
|-------------------|-----------|-------|
| Number of entries | VarInt    |       |
| Entry             | see below |       |

| Name | Type             | Notes                         |
|------|------------------|-------------------------------|
| ID   | VarInt           | Entry ID (must be registered) |
| Data | <type dependent> |                               |

