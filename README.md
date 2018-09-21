# The  problem…

Dependency management in Xcode is … a mixed bag of joy, punctuated with mild annoyance, frustration and wasted time.

Swift’s lack of ABI stability doesn’t help.

There are any number of available options from:
*	Manual.  Either by using a workspace and subprojects or building desired libraries manually and then importing them into the project … or a mixture of both
* CocoaPods
*	Carthage
*	Swift Package Manager (not yet able to support iOS projects)

There are pros and cons to all these.

I’ve been using Carthage for several years (pretty much since it was released) with mixed results.

One frustrating aspect of Carthage is the need to re-build dependent dependencies.  This means if you have a library with a number of dependencies, when you want to update that library, you have to also re-build its dependencies, even if they themselves haven’t changed.

This is, mildly, helped by Carthage’s support of GitHub releases, which can be used to generate a release/archive of the library for a given release version.

The problem with this is it’s actually counter to Carthage’s “independent” philosophy, in that there isn’t a “single” repository (or point of failure).  It also means that, if you need to generate those releases yourself, you will need to fork them and manage it yourself, which is not desirable or fun.

One of the larger issues, which made this even more frustrating, was Swift’s ABI (binary) instability.  Each time a new version of Xcode or Swift was released, all previous binaries were incompatible, meaning that the release binaries would need to be re-generated (for the version of Xcode) and released back to GitHub, which caused no end of issues with version numbering.

# "A" solution…

Recently, was Carthage updated to support “Binary only” releases.  Meaning that it was no longer tied to a source repo, but could be used to store and manage the archives separately.

One of the requirements for this, is to store the binaries on https backed server.  This is kind of annoying, but respected.

Almost all my needs are based on having an internal server capable of managing and maintaining libraries, going out, registering a domain, getter a signed certificate and host it and the binaries was a lot more trouble than it was worth (and in some cases, would violate IT policies).

What I really need was a system I could run locally and which would be easy to maintain and manage and allow a certain amount of freedom when it comes to having to update releases.

While there are a number of possible solutions, including Caddy or even one of the Swift based server projects, I decided to use something which has been around for awhile and has a reasonable level of support for the things I needed to get done…

*	Support for SSL/TLS certificates
*	Support for self signed certificates
*	Support for put, to upload new releases and have them managed in a common manner.

This is why the project is a Java/Web based project, primary focused been run within Tomcat.

# Disclaimer

This is NOT meant to look pretty, it's meant to be fast, simply and work
