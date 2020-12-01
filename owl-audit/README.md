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

2) Get list of IRIs

```sparql
select distinct ?iri where { 
  graph ?graph {} 
  BIND (str(?graph) AS ?iri)
} order by ?iri
```

Save the output as [iris.list](iris.list)

3) Run Audits

```
export RUBYLIB=`pwd`/owl-audit/lib
./owl-audit/tools/run-audits-jena \
    --host localhost \
    --port 3030 \
    --dataset firesat \
    --audit-tree `pwd`/owl-audit/audits/bundle \
    --iri-file `pwd`/owl-audit/iris.list
    --debug \
    > test-bundle.log 2>&1
```

Produces [test-bundle.log](test-bundle.log)

</details>

## Problems

<details>
<summary>Details</summary>

- Missing Ruby gem for `zip` 

  Which version is compatible with jRuby-1.7?
  https://github.com/rubyzip/rubyzip/releases
  
  For example, rubyzip-1.1.7 requires jRuby >= 1.9.2
  
  Without `zip`, we cannot use the option `--report` that would
  create a zip file.
  
- The option `--audit-dir` seems ineffective but `--audit-tree` seems to work.
  
  
</details>
