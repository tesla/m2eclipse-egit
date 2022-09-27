# Maven SCM Handler for EGit

## Materialize Maven projects from Git

See https://books.sonatype.com/m2eclipse-book/reference/creating-sect-importing-projects.html

## Installation
Requires Java 17 to run.

In Eclipse,
  - go to `Help` > `Install New Software...` and add this p2 repository:
  	  * `https://github.com/tesla/m2eclipse-egit/releases/download/0.17.0/` for the latest release (m2e 2.x compatible)
  	  * `https://github.com/tesla/m2eclipse-egit/releases/download/latest/` for the latest CI build (m2e 2.x compatible)
      * `https://tesla.github.io/m2eclipse-egit/updates/` for latest m2e 1.x compatible builds
  - Select `Maven SCM Handler for EGit`
  - Finish the installation
  - Restart Eclipse 


License
=======
[Eclipse Public License, v1.0](http://www.eclipse.org/legal/epl-v10.html)
