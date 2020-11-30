name 'every subclass axiom is essential'

prologue {
  require 'tsort'
  
  class MyHash < Hash
    include TSort
    alias tsort_each_node each_key
    def tsort_each_child(node, &block)
      fetch(node).each(&block) if include?(node)
    end
  end

  @asserted = MyHash.new { |h, k| h[k] = Set.new }
  @inferred = Hash.new { |h, k| h[k] = Set.new }
  @graphs = Hash.new { |h, k| h[k] = Hash.new { |l, m| l[m] = Set.new } }
}

query %q{
  
  <%= @namespace_defs %>

  select distinct ?graph ?sub ?sup

  <%= @from_named_clauses_by_group['named'] %>
    
  where {
    graph ?graph { ?sub rdfs:subClassOf ?sup }
  }
    
}

filter do |r, emit|
  if r
    
    # Accumulate asserted subclass axioms and their graphs.
    
    @asserted[r.sup] << r.sub
    @graphs[r.sup][r.sub] << r.graph
    
  else
    
    # At end of input, calculate inferred subclass axioms.
    
    @asserted.tsort.each do |sup|
      @inferred[sup] = @asserted[sup].inject(Set.new) do |inf, sub|
        inf += @asserted[sub]
        inf += @inferred[sub]
        inf
      end
    end
    
    # Emit asserted axioms.
    
    @asserted.each do |sup, subs|
      subs.each do |sub|
        result = OpenStruct.new
        result.sup = sup
        result.sub = sub
        emit.call(result)
      end
    end
    
    # Indicate end of input.
    
    emit.call(nil)
    
  end
end

predicate do |r|
  if @inferred[r.sup].include?(r.sub)
    text = "Asserted in #{@graphs[r.sup][r.sub].to_a.join(', ')}."
    [false, text]
  else
    [true, nil]
  end
end

case_name { |r| "#{r.sub.to_s} subclass of #{r.sup.to_s}" }