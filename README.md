## Gitclone
This is a project to clone the basic functionality of git, a version control software.

# Usage
init
```bash
java gitlet.Main init
```

add
```bash
java gitlet.Main add [file name]
```

commit
```bash
java gitlet.Main commit [message]
```

rm
```bash
java gitlet.Main rm [file name]
```

log
```bash
java gitlet.Main log
```

global-log
```bash
java gitlet.Main global-log
```

find
```bash
java gitlet.Main find [commit message]
```

status
```bash
java gitlet.Main status
```

checkout
```bash
java gitlet.Main checkout -- [file name]
java gitlet.Main checkout [commit id] -- [file name]
java gitlet.Main checkout [branch name]
```

branch
```bash
java gitlet.Main branch [branch name]
```

rm-branch
```bash
java gitlet.Main rm-branch [branch name]
```

reset
```bash
java gitlet.Main reset [commit id]
```

merge
```bash
java gitlet.Main merge [branch name]
```
