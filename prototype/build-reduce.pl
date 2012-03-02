#!/usr/bin/perl -w
use strict;
use lib ".";
use PAsm;

#nodecnt : number of nodes in the graph.
my $nodecnt = 0;

# the core representation of the graph structure is essentially the same with build-map.pl
# what build-reduce.pl and this function below does is to simplify the graph by counting edge degrees

# This function simplifies each node of the graph and calls another function called print_node from PASM module to print it. Each node is printed as key-value pair. The key being the node id. And the value will all the other nodes with which this node shares an edge along with other details. 
sub simplify_and_print
{
  #$nodeid represents a set of rows with a specific K-mer we processed and send off
  #here for printing
  my $nodeid = shift;
  my $node = shift;

  return if !defined $nodeid;

  $nodecnt++;

  my $seq = dna2str($nodeid);	## converts ascii representation of node to sequence form. The function is called from PASM module.
  my $rc  = rc($seq);

  node_setstr_raw($node, $nodeid);  

  $seq = substr($seq, 1);
  $rc  = substr($rc,  1);

  foreach my $x (qw/f r/)
  {
    my $degree = 0;
    my @thread;

    foreach my $y (qw/f r/)
    {
      my $t = "$x$y";

      if (exists $node->{$t})
      {
          #number of edges on the node of the graph. It represents how many different nodes (neighboors: A/C/G/T) 
          #we can we find in each direction the edge points to
        $degree += scalar keys %{$node->{$t}};
      }
    }

    foreach my $y (qw/f r/)
    {
      my $t = "$x$y";

      if (exists $node->{$t})
      {
        my @vs;

        # $vc are the neighboor nucleotides over the graph link
        foreach my $vc (keys %{$node->{$t}})
        {
          #attach attibute to $seq (the : operator) its reverse-complement if the first direction of the node is forward
          my $v = ($x eq $fwd) ? $seq : $rc;

          $v .= $vc;
          $v = rc($v) if ($y eq $rev);

          my $link = str2dna($v); ## Stores which other node the given node forms an edge with.

          push @vs, $link;

          # last column in 00-basic.txt with "edge direction:K-MER:read_id"
          if ($THREADREADS && ($degree > 1))
          {
            foreach my $r (@{$node->{$t}->{$vc}})
            {
              push @{$node->{$THREAD}}, "$t:$link:$r";
            }
          }
        }
	## @vs stores all the links to the given nodes.
        $node->{$t} = \@vs;
      }
    }
  }

  print_node($nodeid, $node);
}

my $node;
my $nodeid = undef;

while (<>)
{
  chomp;
  
  #Here we are guaranteed to have all identical k-mers ($curnode) streaming in together
  #This is because Hadoop sorts the output of the map phase before it passes it to the reduce 
  #my ($curnode, $type, $neighbor, $tag) = split /\t/, $_;
  my ($curnode, $type, $neighbor, $tag, $state) = split /\t/, $_;

  #first row of data coming in it skips because there is no $nodeid defined
  #then once a row with a different K-mer comes we send it off to processing
  #(remember rows are sorted by K-mer, so we are getting sets of rows with the same K-mer)
  if ((defined $nodeid) && ($curnode ne $nodeid))
  {
    simplify_and_print($nodeid, $node);   ## When a new nodeid comes the earlier node (data structure) formed from identical nodeid is send for printing.
    $node = undef;
  }
  ## $curnode stores the current node.
  $nodeid = $curnode;

  #Here we build the core data structure that stores our graph, as a 3-layered hash {key->{key->{key->[array of read ids]}}}

  #{ K-mer -> direction (ff/fr/rf/rr) -> neighbooring k-mer (A/C/G/T) } - {[ read_id ]


  if ($THREADREADS)
  {
    #if we want to allow multiple reads threading through the each node of the graph
    if ((!defined $node->{$type}->{$neighbor}) ||
        (scalar @{$node->{$type}->{$neighbor}} < $MAXTHREADREADS))
    {
      #three level hash with array as value
      push @{$node->{$type}->{$neighbor}}, $tag;
    }
  }
    #once we go over the number of reads allowed to thread (collapse) over a node we re-set
    #the data structure
  else
  {
    $node->{$type}->{$neighbor} = 1;
  }

  ## $MERTAG stores the Read id of the earliest reads which threads through that node.
  if ((!defined $node->{$MERTAG}) || ($tag lt $node->{$MERTAG}->[0]))
  {
    $node->{$MERTAG}->[0] = $tag;
  }

  #the states save where we are on the read (5', 3' etc). Based on that calculate coverage
  if ($state ne "i")
  {
    $node->{$COVERAGE}->[0]++;

    if ($state eq "5" || $state eq "6")
    {
      if ((!defined $node->{$R5}) ||
          (scalar @{$node->{$R5}} < $MAXR5))
      {
        if ($state eq "6")
        {
          my $pos = $K-1;
          push @{$node->{$R5}}, "~$tag:$pos";
        }
        else
        {
          push @{$node->{$R5}}, "$tag:0";
        }
      }
    }
  }
}

simplify_and_print($nodeid, $node);

hadoop_counter("nodecount", $nodecnt);
