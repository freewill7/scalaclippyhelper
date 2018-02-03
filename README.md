# scalaclippyhelper

A simple utility (written in Scala) for reading exported data from the old SFClippy  and storing it in the new sfclippy firebase reference.

## Build instructions

### Compile

```
sbt build
```

## Running instructions

The app assumes a "serviceAccount.json" file in the current directory (from firebase admin console).

### Run

```
sbt "run [user_id] [path_to_exported_data]"
```

### Repl

```
sbt
...
build
...
run [user_id] [path_to_exported_data]
```
