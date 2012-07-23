# Pallet crate for postgres

This a crate to install and run postgres via [Pallet](http://pallet.github.com/pallet).

[Release Notes](https://github.com/pallet/postgres-crate/blob/master/ReleaseNotes.md)

## Server Spec

The postgres crate defines the `postgres` function, that takes a settings map
and returns a server-spec for installing postgres.

## Settings

The postgres crate uses the following settings:

* `:version`
  a string to specify the point version of PostgreSQL (e.g., `"9.1"`). The default is the version provided by the system's packaging system

* `:components`
  a set of one or more recognized keywords. The set of every component is `#{:server :libs :client}`.

* `:strategy`
  allows override of the install strategy (`:packages`, `:package-source`, or `:rpm`)

* `:packages`
  the packages that are used to install

* `:package-source`
  a non-default package source for the packages

* `:rpm`
  takes a map of [`remote-file` options](http://palletops.com/pallet/api/0.7/pallet.action.remote-file.html) specifying an RPM file to install

* `:default-cluster-name`
  name of the default cluster created by the installer

* `:bin`
  path to binaries

* `:owner`
  unix owner for Postgres files

* `:postgresql_file`
  path to `postgresql.conf`

* `:has-pg-wrapper`
  boolean flag for availability of a wrapper allowing command execution against a specified cluster.

* `:has-multicluster-service`
  boolean flag specifying whether the init service is multi-cluster capable.

* `:initdb-via`
  whether to use the initdb (`:initdb`), or service (`:service`) to run initdb

* `:options`
  A map of options:
  - `:data_directory`
    path to storage location
  - `:hba_file`
    path to `pg_hba.conf` location
  - `:ident_file`
    path to `pg_ident.conf` location
  - `:external_pid_file`
    path to pid file
  - `:unix_socket_directory`
    path to directory for unix sockets

## Support

[On the group](http://groups.google.com/group/pallet-clj), or
[#pallet](http://webchat.freenode.net/?channels=#pallet) on freenode irc.

## License

Licensed under [EPL](http://www.eclipse.org/legal/epl-v10.html)

Copyright 2010, 2011, 2012 Hugo Duncan.
