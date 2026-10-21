# Terraform commands

Command set native to terraform. Always uses a single slash, whereas compatibility commands use a double slash (like
native WorldEdit). All terraform commands are also aliased as `/tf:command`.

The command syntax below is as follows:

- All commands are written as they would be typed in-game, eg prefixed with a slash (`/`).
- Required arguments are written in `<angle brackets>`.
- Optional arguments are written in `[square brackets]`.
- Arguments that can be one of multiple values are written as `one|two|three`.
- Arguments that are zero or more values are written as `...`.
- Arguments that are one or more values are written as `<...>`.
-

## General

- `/terraform` - returns some info about terraform and the installed extensions (todo)
- `/terraform debug` - returns some debug info

## Selection

- `/pos1 [xyz] [selection]`
- `/pos2 [xyz] [selection]`
- `/hpos1 [selection]`
- `/hpos2 [selection]`


- `/sel list`
- `/sel clear [selection]`
- `/sel type cuboid|etc [selection]`
- `/sel expand <amount>` - also the other ops like contract and stuff

## Clipboard

- `/copy [mask] from [selection] to [clipboard]`
- `/paste [mask] [clipboard]`
- `/c list` - lists the existing clipboards
- `/c clear [clipboard]` - clears the clipboard, or the default clipboard if unspecified
- `/c rotate <angle> [axis] [clipboard]`
- `/c flip [axis] [clipboard]`

## Blah
