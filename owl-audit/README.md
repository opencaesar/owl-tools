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
    --audit-dir `pwd`/owl-audit/audits \
    --iri-file `pwd`/owl-audit/iris.list
    --debug
```

Produces:

```
io/console on JRuby shells out to stty for most operations
log4j:WARN No appenders could be found for logger (com.hp.hpl.jena.util.FileManager).
log4j:WARN Please initialize the log4j system properly.
log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: Start of run-audits-jena.
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: IRIs: /opt/local/github.opencaesar/owl-tools/owl-audit/iris.list
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: Fuseki: localhost:3030/firesat
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: m[data] = http://localhost:3030/firesat/data
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: m[query] = http://localhost:3030/firesat/query
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: m[update] = http://localhost:3030/firesat/update
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: open_data_service: uri=http://localhost:3030/firesat/data
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: get namespace definitions
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: rdf = http://www.w3.org/1999/02/22-rdf-syntax-ns#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: rdfs = http://www.w3.org/2000/01/rdf-schema#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: owl = http://www.w3.org/2002/07/owl#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: xsd = http://www.w3.org/2001/XMLSchema#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: xml = http://www.w3.org/XML/1998/namespace
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: dc = http://purl.org/dc/elements/1.1/
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: swrl = http://www.w3.org/2003/11/swrl#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: set nsPrefix: swrlb = http://www.w3.org/2003/11/swrlb#
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: namespace_by_prefix: {"rdf"=>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"}
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: construct sparql prefixes
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: namespace_defs: PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: load imports graph
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: load imports graph from repository models
D, [2020-11-30 16:26:38 #2714149] DEBUG -- run-audits-jena: query: 
            select ?imported
            from <http://imce.jpl.nasa.gov/discipline/fse/assemblies>
            where { <http://imce.jpl.nasa.gov/discipline/fse/assemblies> <http://www.w3.org/2002/07/owl#imports> ?imported }
          
F, [2020-11-30 16:26:38 #2714149] FATAL -- run-audits-jena: Detected an exception. Stopping ... Not Found
Error 404: Not Found
 (Java::ComHpHplJenaSparqlEngineHttp::QueryExceptionHTTP)
com.hp.hpl.jena.sparql.engine.http.HttpQuery.execCommon(com/hp/hpl/jena/sparql/engine/http/HttpQuery.java:444)
com.hp.hpl.jena.sparql.engine.http.HttpQuery.execGet(com/hp/hpl/jena/sparql/engine/http/HttpQuery.java:289)
com.hp.hpl.jena.sparql.engine.http.HttpQuery.exec(com/hp/hpl/jena/sparql/engine/http/HttpQuery.java:240)
com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP.execSelect(com/hp/hpl/jena/sparql/engine/http/QueryEngineHTTP.java:302)
jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
jdk.internal.reflect.NativeMethodAccessorImpl.invoke(jdk/internal/reflect/NativeMethodAccessorImpl.java:62)
jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(jdk/internal/reflect/DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:566)
RUBY.run_select_query(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:311)
RUBY.load_imports(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:175)
RUBY.run(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:65)
$_dot_.tools.run_minus_audits_minus_jena.run(./tools/run-audits-jena:88)
$_dot_.tools.run_minus_audits_minus_jena.run($_dot_/tools/./tools/run-audits-jena:88)
RUBY.start(/home/rouquette/.rvm/gems/jruby-1.7.27/gems/logger-application-0.0.2/lib/logger/application.rb:67)
$_dot_.tools.run_minus_audits_minus_jena.(root)(./tools/run-audits-jena:109)
$_dot_.tools.run_minus_audits_minus_jena.(root)($_dot_/tools/./tools/run-audits-jena:109)
I, [2020-11-30 16:26:38 #2714149]  INFO -- run-audits-jena: End of run-audits-jena. (status: -1)

```

</details>
