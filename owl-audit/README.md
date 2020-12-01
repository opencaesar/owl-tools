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
    --host http://localhost \
    --port 3030 \
    --dataset firesat \
    --audit-dir `pwd`/owl-audit/audits \
    --iri-file `pwd`/owl-audit/iris.list
    --debug
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
I, [2020-11-30 16:02:38 #2704017]  INFO -- run-audits-jena: Start of run-audits-jena.
D, [2020-11-30 16:02:38 #2704017] DEBUG -- run-audits-jena: IRIs: /opt/local/github.opencaesar/owl-tools/owl-audit/iris.list
D, [2020-11-30 16:02:38 #2704017] DEBUG -- run-audits-jena: Fuseki: http://localhost:3030/firesat
I, [2020-11-30 16:02:38 #2704017]  INFO -- run-audits-jena: get namespace definitions
F, [2020-11-30 16:02:38 #2704017] FATAL -- run-audits-jena: Detected an exception. Stopping ...  (Java::JavaLang::NullPointerException)
com.hp.hpl.jena.rdf.model.impl.ModelCom.getPrefixMapping(com/hp/hpl/jena/rdf/model/impl/ModelCom.java:959)
com.hp.hpl.jena.rdf.model.impl.ModelCom.withDefaultMappings(com/hp/hpl/jena/rdf/model/impl/ModelCom.java:1003)
com.hp.hpl.jena.rdf.model.impl.ModelCom.<init>(com/hp/hpl/jena/rdf/model/impl/ModelCom.java:74)
com.hp.hpl.jena.rdf.model.impl.ModelCom.<init>(com/hp/hpl/jena/rdf/model/impl/ModelCom.java:70)
com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph(com/hp/hpl/jena/rdf/model/ModelFactory.java:176)
org.apache.jena.web.DatasetAdapter.getModel(org/apache/jena/web/DatasetAdapter.java:40)
jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
jdk.internal.reflect.NativeMethodAccessorImpl.invoke(jdk/internal/reflect/NativeMethodAccessorImpl.java:62)
jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(jdk/internal/reflect/DelegatingMethodAccessorImpl.java:43)
java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:566)
RUBY.get_namespaces(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:133)
org.jruby.RubyHash.each(org/jruby/RubyHash.java:1342)
RUBY.get_namespaces(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:132)
RUBY.run(/opt/local/github.opencaesar/owl-tools/owl-audit/lib/Application/JenaApplication.rb:63)
$_dot_.tools.run_minus_audits_minus_jena.run(./tools/run-audits-jena:88)
$_dot_.tools.run_minus_audits_minus_jena.run($_dot_/tools/./tools/run-audits-jena:88)
RUBY.start(/home/rouquette/.rvm/gems/jruby-1.7.27/gems/logger-application-0.0.2/lib/logger/application.rb:67)
$_dot_.tools.run_minus_audits_minus_jena.(root)(./tools/run-audits-jena:109)
$_dot_.tools.run_minus_audits_minus_jena.(root)($_dot_/tools/./tools/run-audits-jena:109)
I, [2020-11-30 16:02:38 #2704017]  INFO -- run-audits-jena: End of run-audits-jena. (status: -1)

```

</details>
