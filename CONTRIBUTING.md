# Contributor Guidelines

## General Workflow

This is the process for committing code into master. There are of course exceptions to these rules, for example minor changes to comments and documentation, fixing a broken build etc.

1. Make sure you have signed the [Typesafe CLA](http://www.typesafe.com/contribute/cla), if not, sign it online.
2. You should always perform your work in a Git feature branch or
GitHub fork. The branch should be given a descriptive name that explains its intent.
3. When the feature or fix is completed you should open a [Pull Request](https://help.github.com/articles/using-pull-requests) on GitHub.
4. The Pull Request should be reviewed (and merged when ready) by
one of the project maintainers. Anyone should feel free to chime
in on pull requests.
5. After the review you should fix the issues as needed (pushing a new commit for new review etc.), iterating until the reviewers give their thumbs up.
6. Once the code has passed review the Pull Request can be merged
into the master branch. The feature branch should be deleted at
that time.

## Pull Request Requirements

For a Pull Request to be considered at all it has to meet these requirements:

1. Live up to the current code standard:
   - Not violate [DRY](http://programmer.97things.oreilly.com/wiki/index.php/Don%27t_Repeat_Yourself).
   - [Boy Scout Rule](http://programmer.97things.oreilly.com/wiki/index.php/The_Boy_Scout_Rule) needs to have been applied.
2. There should be tests if feasible and appropriate. For
Activator we don't have a good way to test the HTML/JS UI unfortunately.
3. Copyright:
   -  All source files in the project must have a Typesafe copyright header in the style of ``Copyright (C) 2011-2013 Typesafe Inc. <http://www.typesafe.com>``. Never delete or change existing copyright notices, just add additional info.

If these requirements are not met then the code should **not** be merged into master, or even reviewed - regardless of how good or important it is. No exceptions.

## External Dependencies

All the external runtime dependencies for the project, including
transitive dependencies, must have an open source license that is
equal to, or compatible with,
[Apache 2](http://www.apache.org/licenses/LICENSE-2.0).

## Creating Commits And Writing Commit Messages

Follow these guidelines when creating public commits and writing commit messages.

1. If your work spans multiple local commits (for example; if you do safe point commits while working in a feature branch or work in a branch for long time doing merges/rebases etc.) then please do not commit it all but rewrite the history by squashing the commits into a single big commit which you write a good commit message for (like discussed in the following sections). For more info read this article: [Git Workflow](http://sandofsky.com/blog/git-workflow.html). Every commit should be able to be used in isolation, cherry picked etc.
2. First line should be a descriptive sentence what the commit is doing. It should be possible to fully understand what the commit does by just reading this single line. It is **not ok** to only list the ticket number, type "minor fix" or similar. Include reference to ticket number, prefixed with #, at the end of the first line. If the commit is a small fix, then you are done. If not, go to 3.
3. Following the single line description should be a blank line followed by an enumerated list with the details of the commit.
4. Add keywords for your commit (depending on the degree of automation we reach, the list may change over time):
    * ``Review by @gituser`` - if you want to notify someone on the team. The others can, and are encouraged to participate.
    * ``Fix/Fixing/Fixes/Close/Closing/Refs #ticket`` - if you want to mark the ticket as fixed in the issue tracker (Assembla understands this).
    * ``backport to _branch name_`` - if the fix needs to be cherry-picked to another branch (like 2.9.x, 2.10.x, etc)

Example:

    Adding monadic API to Future. Fixes #2731

      * Details 1
      * Details 2
      * Details 3
