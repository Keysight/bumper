# 0.1.6

- Fix bug where kind=separate options were not printed as separate. (Thanks Diego!)
- Fix bug where toArguments was joining separate arguments into a " "-separated single argument.
- Better separate various exports of commands toPOSIXArguments, toWinArguments, toExecArguments, toArguments.
- Add pretty-printers for Include paths and Includes.

# 0.1.5

- Minor api improvements
- Add model of include paths

# 0.1.4

- Model executable in compilation database entries because they need to be written to disk
  when serializing.

# 0.1.3

- Expose API to analyze a plain cdb so that caller can be in charge of deserializing json.

# 0.1.2
# 0.1.1

- CLI wrapper with `dobby validate` and `dobby info` commands.

# 0.1.0

Initial release
