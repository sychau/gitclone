## Gitclone
This is a project to clone the functionality of git.

# Usage
init
java gitlet.Main init

add
java gitlet.Main add [file name]

commit
java gitlet.Main commit [message]

rm
java gitlet.Main rm [file name]

log
java gitlet.Main log

global-log
java gitlet.Main global-log

find
java gitlet.Main find [commit message]

status
java gitlet.Main status

checkout
java gitlet.Main checkout -- [file name]
java gitlet.Main checkout [commit id] -- [file name]
java gitlet.Main checkout [branch name]

branch
java gitlet.Main branch [branch name]

rm-branch
java gitlet.Main rm-branch [branch name]

reset
java gitlet.Main reset [commit id]

merge
java gitlet.Main merge [branch name]

