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

4) Select jRuby 1.7.27

```zsh
rvm use jruby-1.7.27
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

    Build the library (`./gradlew owl-audit:build`) then invoke the jar file like this:
  
    ```
    java -jar <path to owl-audit-<version>.jar --help
    ```
  
    Alternatively, run the audits from "source" after setting up the Ruby environment as follows:
    
    ```
    cd owl-audit
    export RUBYLIB=`pwd`/src/main/resources/audit-framework:`pwd`/src/main/resources/rubygems/logger-application-0.0.2/lib
    ```

4) Examples

  <details>
  <summary>audits/bundle</summary>
  
  ```
  ./src/main/resources/audit-framework/tools/run-audits-jena-cli \
      --host localhost \
      --port 3030 \
      --dataset firesat \
      --audit-tree `pwd`/audits/bundle \
      --iri-file `pwd`/iris.list \
      --prefix-file `pwd`/prefixes.yaml \
      --output-file `pwd`/test-bundle.xml
  ```
  
  
  See [test-bundle.xml](test-bundle.xml)
  
  </details>
  
  
  <details>
  <summary>audits/non-recurring</summary>
  
  
  ```
  ./src/main/resources/audit-framework/tools/run-audits-jena-cli \
      --host localhost \
      --port 3030 \
      --dataset firesat \
      --audit-tree `pwd`/audits/non-recurring \
      --iri-file `pwd`/iris.list \
      --prefix-file `pwd`/prefixes.yaml \
      --output-file `pwd`/test-non-recurring.xml
  ```
  
  See [test-non-recurring.xml](test-non-recurring.xml)
  
  </details>
  
  
  <details>
  <summary>audits/group/special</summary>
  
  ```
  ./src/main/resources/audit-framework/tools/run-audits-jena-cli \
      --host localhost \
      --port 3030 \
      --dataset firesat \
      --audit-tree `pwd`/audits/group/special \
      --iri-file `pwd`/iris.list \
      --prefix-file `pwd`/prefixes.yaml \
      --output-file `pwd`/test-group-special.xml
  ```
  
  See [test-group-special.xml](test-group-special.xml)
  
  </details>
  
  
  <details>
  <summary>audits/group/all/other</summary>
  
  ```
  ./src/main/resources/audit-framework/tools/run-audits-jena-cli \
      --host localhost \
      --port 3030 \
      --dataset firesat \
      --audit-tree `pwd`/audits/group/all/other \
      --iri-file `pwd`/iris.list \
      --prefix-file `pwd`/prefixes.yaml \
      --output-file `pwd`/test-group-all-other.xml
  F, [2020-12-01 12:01:16 #2879080] FATAL -- run-audits-jena: Detected an exception. Stopping ... 
  Exception Occurred: undefined method `map' for nil:NilClass.
  Backtrace:
  - (erb):50:in `result'
  - org/jruby/RubyKernel.java:1079:in `eval'
  - /home/rouquette/.rvm/rubies/jruby-1.7.27/lib/ruby/1.9/erb.rb:838:in `result'
  - /opt/local/github.opencaesar/owl-tools/owl-audit/lib/Audit/JenaAudit.rb:75:in `run'
  - /opt/local/github.opencaesar/owl-tools/owl-audit/lib/Audit/JenaAudit.rb:175:in `start'
  - org/jruby/RubyKernel.java:1479:in `loop'
  - /opt/local/github.opencaesar/owl-tools/owl-audit/lib/Audit/JenaAudit.rb:171:in `start' (RuntimeError)
  /opt/local/github.opencaesar/owl-tools/owl-audit/lib/Audit/JenaAudit.rb:179:in `start'
  org/jruby/RubyKernel.java:1479:in `loop'
  /opt/local/github.opencaesar/owl-tools/owl-audit/lib/Audit/JenaAudit.rb:171:in `start'
  ```
  
  </details>
  
  
  <details>
  <summary>audits/group/all/no-embedding</summary>
  
  ```
  ./src/main/resources/audit-framework/tools/run-audits-jena-cli \
      --host localhost \
      --port 3030 \
      --dataset firesat \
      --audit-tree `pwd`/audits/group/all/no-embedding \
      --iri-file `pwd`/iris.list \
      --prefix-file `pwd`/prefixes.yaml \
      --output-file `pwd`/test-group-all-no-embedding.xml
  .
  F, [2020-12-01 12:02:09 #2879610] FATAL -- run-audits-jena: Detected an exception. Stopping ... 
  Exception Occurred: Line 869, column 66: Unresolved prefixed name: owl2-mof2-backbone:topReifiedStructuredDataPropertySource.
  Backtrace:
  - com.hp.hpl.jena.sparql.lang.ParserBase.throwParseException(com/hp/hpl/jena/sparql/lang/ParserBase.java:661)
  - com.hp.hpl.jena.sparql.lang.ParserBase.resolvePName(com/hp/hpl/jena/sparql/lang/ParserBase.java:274)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.PrefixedName(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:4888)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.iri(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:4872)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GraphTerm(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3389)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.VarOrTerm(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3331)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GraphNodePath(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3287)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ObjectPath(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:2793)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ObjectListPath(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:2774)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.PropertyListPathNotEmpty(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:2705)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.TriplesSameSubjectPath(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:2649)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.TriplesBlock(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1819)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GroupGraphPatternSub(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1740)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GroupGraphPattern(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1702)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ExistsFunc(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:4412)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.BuiltInCall(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:4329)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.PrimaryExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3881)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.UnaryExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3802)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.MultiplicativeExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3669)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.AdditiveExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3567)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.NumericExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3560)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.RelationalExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3492)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ValueLogical(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3485)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ConditionalAndExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3476)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.ConditionalOrExpression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3443)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.Expression(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:3436)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.Bind(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1945)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GraphPatternNotTriples(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1891)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GroupGraphPatternSub(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1765)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.GroupGraphPattern(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:1702)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.WhereClause(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:446)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.SelectQuery(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:134)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.Query(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:50)
  - com.hp.hpl.jena.sparql.lang.sparql_11.SPARQLParser11.QueryUnit(com/hp/hpl/jena/sparql/lang/sparql_11/SPARQLParser11.java:41)
  - com.hp.hpl.jena.sparql.lang.ParserSPARQL11$1.exec(com/hp/hpl/jena/sparql/lang/ParserSPARQL11.java:49)
  - com.hp.hpl.jena.sparql.lang.ParserSPARQL11.perform(com/hp/hpl/jena/sparql/lang/ParserSPARQL11.java:98)
  - com.hp.hpl.jena.sparql.lang.ParserSPARQL11.parse$(com/hp/hpl/jena/sparql/lang/ParserSPARQL11.java:53)
  - com.hp.hpl.jena.sparql.lang.SPARQLParser.parse(com/hp/hpl/jena/sparql/lang/SPARQLParser.java:37)
  - com.hp.hpl.jena.query.QueryFactory.parse(com/hp/hpl/jena/query/QueryFactory.java:156)
  - com.hp.hpl.jena.query.QueryFactory.create(com/hp/hpl/jena/query/QueryFactory.java:79)
  - com.hp.hpl.jena.query.QueryFactory.create(com/hp/hpl/jena/query/QueryFactory.java:52)
  - com.hp.hpl.jena.query.QueryFactory.create(com/hp/hpl/jena/query/QueryFactory.java:40)
  - com.hp.hpl.jena.query.ParameterizedSparqlString.asQuery(com/hp/hpl/jena/query/ParameterizedSparqlString.java:1384)
  - jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(jdk/internal/reflect/DelegatingMethodAccessorImpl.java:43)
  - java.lang.reflect.Method.invoke(java/lang/reflect/Method.java:566)
  
  ```
  
  </details>

</details>

## Debugging Audit rules

<details>
<summary>Details</summary>

Suppose audits are executed like this:

```
java -jar <path>/owl-audit-<version>.jar \
  --host localhost \
  --port 3030 \
  --dataset firesat \
  --audit-tree ./src/audits/bundle \
  --iri-file iris.list \
  --prefix-file prefixes.yaml \
  --output-file tests.xml 
```

To debug the Audit rules, execute with the `--debug` option and save the output to a log file:

```
java -jar <path>/owl-audit-<version>.jar \
  --host localhost \
  --port 3030 \
  --dataset firesat \
  --audit-tree ./src/audits/bundle \
  --iri-file iris.list \
  --prefix-file prefixes.yaml \
  --output-file tests.xml \
  --debug > tests.log 2>&1
```

The log file, `tests.log`, will show multiple sequences of the following:

- A separating log entry of `=` signs.
- The SPARQL endpoint.
- The SPARQL query.
- Log entries for each solution.

For example:

```
D, [2020-12-06 15:04:26 #584063] DEBUG -- run-audits-jena.rb: ===============
D, [2020-12-06 15:04:26 #584063] DEBUG -- run-audits-jena.rb: SPARQL endpoint: http://localhost:3030/firesat
D, [2020-12-06 15:04:26 #584063] DEBUG -- run-audits-jena.rb: SPARQL query:
SELECT  ?imported
FROM <http://imce.jpl.nasa.gov/discipline/fse/assemblies>
WHERE
  { <http://imce.jpl.nasa.gov/discipline/fse/assemblies> <http://www.w3.org/2002/07/owl#imports> ?imported }

D, [2020-12-06 15:04:26 #584063] DEBUG -- run-audits-jena.rb: solution: ( ?imported = <http://imce.jpl.nasa.gov/discipline/fse/fse> )
```

The SPARQL query shown in the log corresponds to the expansion of the DSL audit query.
Copying the SPARQL query and pasting it in a Fuseki web interface facilitates debugging 
the logic of a DSL audit query.

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
