#!/usr/bin/perl -w
use strict;
use lib ".";
use PAsm;

while (<>)
{
  chomp;

  my $node = {};

  my @vals = split /\t/, $_;

  my $nodeid = shift @vals;

  my $msgtype = shift @vals; ## nodemsg

  if ($msgtype eq $NODEMSG)
  {
    parse_node($node, \@vals); ##Stores the node in a hash data structure.Each value for the key in the hash is stored in hash form(key- value[array]).
    #[node CA:
    #v(coverage)-->1.00
    #rr-->GF  (reverse of CA overlaps with reverse of GF)
    #s-->CA
    #]
  }
  else
  {
    die "Unknown msg: $_\n";
  }

  print_node($nodeid, $node, 1); ##node printed sorted and send to quickmerge-reduce.pl.
}
