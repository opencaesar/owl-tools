# owl-audit

## Ruby

<details>
<summary>Details</summary>

The audit framework was originally written in jRuby 1.7

To install:

1) Create a file: `~/.rvmrc` with the following:

```
rvm_silence_path_mismatch_check_flag=1
rvm_ignore_gemsets_flag=1
```

2) Install RVM

```zsh
curl -sSL https://get.rvm.io | bash -s stable --ruby=jruby-1.7 --without-gems="gem-wrappers rubygems-bundler rake bundler"
```

3) Update `~/.zshrc`

```zsh
export PATH="$PATH:$HOME/.rvm/bin"
source $HOME/.rvm/scripts/rvm
```

4) Configure jRuby

```zsh
rvm use jruby-1.7.27
gem install logger-application
```

</details>

## Running

<details>
<summary>Details</summary>

1) Load Fuseki

- Start fuseki
- Load OML data (eg., use the firesat-example, execute the gradle task `omlLoad`)

2) Run Audits

```
export RUBYLIB=`pwd`/owl-audit/lib
./owl-audit/tools/run-audits-jena \
    --host localhost \
    --port 3030 \
    --dataset firesat \
    --audit-dir `pwd`/owl-audit/audits \
    --iri-file `pwd`/owl-audit/iris.list
```

Produces:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by jnr.posix.JavaLibCHelper to method sun.nio.ch.SelChImpl.getFD()
WARNING: Please consider reporting this to the maintainers of jnr.posix.JavaLibCHelper
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
io/console on JRuby shells out to stty for most operations
log4j:WARN No appenders could be found for logger (com.hp.hpl.jena.util.FileManager).
log4j:WARN Please initialize the log4j system properly.
log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
<testsuites/>
```

</details>
