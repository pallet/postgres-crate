## Usage

The `server-spec` function provides a convenient pallet server spec for
postgres.  It takes a single map as an argument, specifying configuration
choices, as described below for the `settings` function.  You can use this
in your own group or server specs in the :extends clause.

```clj
(require '[pallet/crate/postgres :as postgres])
(group-spec my-postgres-group
  :extends [(postgres/server-spec {})])
```

While `server-spec` provides an all-in-one function, you can use the individual
plan functions as you see fit.

The `settings` function provides a plan function that should be called in the
`:settings` phase.  The function puts the configuration options into the pallet
session, where they can be found by the other crate functions, or by other
crates wanting to interact with postgres.

The `install` function is responsible for actually installing postgres.

The `configure` function writes the postgres configuration file, using the form
passed to the :config key in the `settings` function.


## Settings

The postgres crate uses the following settings:

* `:version` 
  a string to specify the point version of PostgreSQL (e.g., `"9.1"`). The
  default is the version provided by the system's packaging system

* `:components`
  a set of one or more recognized keywords. The set of every component is
  `#{:server :libs :client}`.

* `:strategy`
  allows override of the install strategy (`:packages`, `:package-source`, or
  `:rpm`)

* `:packages`
  the packages that are used to install

* `:package-source`
  a non-default package source for the packages

* `:rpm`
  takes a map of
  [`remote-file` options](http://palletops.com/pallet/api/0.7/pallet.action.remote-file.html)
  specifying an RPM file to install

* `:default-cluster-name`
  name of the default cluster created by the installer

* `:bin`
  path to binaries

* `:owner`
  unix owner for Postgres files

* `:postgresql_file`
  path to `postgresql.conf`

* `:has-pg-wrapper`
  boolean flag for availability of a wrapper allowing command execution against
  a specified cluster.

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
