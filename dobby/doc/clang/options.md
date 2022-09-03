# Clang commandline options

Like all compilers, Clang accepts a billion options at the commandline.
We need to parse those options to some extent, so that we can modify clang commands
obtained from a compilation database to suit our needs.

To parse these options is already quite challenging, due to the sheer number, and
all the aliases and variations that exist. For example, to set the output file,
the following invocations are supported by Clang:

```bash
clang -o <filename>
# without a space is also supported:

clang -o<filename>
# but for some values for <filename>, it is ambiguous whether the user meant to specify a filename,
# or whether the user meant another flag. For example: -objcmt-migrate-all.
# In those cases, clang disambiguates to the flag.

# Other versions include:
clang --output <filename>
clang --output=<filename>
clang -Fo <filename>
clang /Fo <filename>
# each with their own complications...
```

Another relevant example is the `-mcpu=..` flag that we need to filter out when producing
code for a certain execution environment. This flag has all kinds of possible values, but
clang also defines aliases for some of those values, for example: `-mv5` aliases `-mcpu=hexagonv5`.

Luckily, Clang has a formal 
[specification of the command line interface](https://github.com/llvm/llvm-project/blob/release/11.x/clang/include/clang/Driver/Options.td).
This specification is written in a LLVM DSL called [TableGen](https://llvm.org/docs/TableGen/).
The definition is used to generate the documentation, as well as C(++) includes defining the relevant structs.

We can invoke `clang-tblgen` on the specification to produce expanded definitions of all commandline options:

```bash
# in the llvm project:
$ cd llvm/include
$ clang-tblgen ../../clang/include/clang/Driver/Options.td --dump-json > clang-options.json
...
```
The outputted json contains a lot of relevant info and is a highly structured representation.
We can read this option specification file and turn it into parsers for various clang options.

# Notes on the Spec

## Spec names vs keys

Every option has a unique key and a non-unique name. The name is what we see at the commandline.
When the spec references another option (for example when specifying the aliases), then it uses
the key.

## CC1Options (TODO)

Some options in the spec are intended to be passed as -Xclang <option..>, I think.
These options are currently not distinguished by dobby. It is unclear if this is bad.

## Option names and the aliasing mess

Option names in the specification are not unique. For example, the options with the
keys `O` and `O_flag` have the same field `"Name": "O"`. 
This is unambiguous, because they have non-overlapping usages:

- `O_flag` is passed as `-O` and aliases the option with key `-O` with `AliasArg: ["1"]`
- `O` is an option with kind JOINED, and passed as `-O<arg>`

The options `O1`--`O4` are distinguished in the spec and have different names, and
are not specified to be aliases of `O`. The flag `--optimize` is specified to be an alias of `-O`
and has `KIND_FLAG`, whereas the flag `--optimize=` is also specified as an alias of `-O`,
having `KIND_JOINED`.

Declared aliases are not transitive. It would be helpful to compute closures of declared
aliases. Yet, be careful to assume that this is the end of the story.
If you want to filter out all optimization flags, you should be careful to filter all
aliases of `-O` *plus* `-O1`--`-O4`, because the latter will not be in the same alias closure.
