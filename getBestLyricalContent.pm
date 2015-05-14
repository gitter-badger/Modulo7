#!/usr/bin/perl
use strict;
use warnings;

# This file contains a program that reads the lyrics file generated by music Score Fetch
# and then asks user for input line. Then on the basis of similarity between input sentence
# and songs computed we compute the best song possible, kind of like a rudimentary processor

# A hashmap which stores the frequency of the lyrics the key for the data structure is 
# the song number and the value is 
my @lyrics_frequencies = ();

# Contains both hindi and english stop words
my @stopWords = ("and", "the", "or", "to", "ki", "tu");

# A hashmap of all the stop words in songs
my %stoplist_hash = ();

# This map stores the similarities between songs and query
my %similarity_map = ();

# Takes an input sentence from user of lyrical content to input query
my %input_sentence_hash = ();

# Song number song file name map 
my %song_num_name_hash = ();

# The number of songs in the analysis
my $song_number = 1;

# Construct the stop list hash
foreach my $word (@stopWords)
{
    $stoplist_hash{$word} = 1;
}

# Method to read the lyrics files and compute the lyrics frequency hash map
sub readLyricsFiles {

    # Sorted to maintain ordering consistency
    my @notestream_files = sort glob("*.lyrics");
    
    # Read each file and construct the lyric frequency hash
    foreach my $filename (@notestream_files) {
    
        open(my $fh, $filename) or die "Could not open file '$filename' $!";
         
        while (my $line = <$fh>) {
            chomp $line;
          
            my @lyricsArray = split /\s+/, $line;
          
            # Populate the lyrics frequency hash
            foreach my $word (@lyricsArray)
            {
                if (! exists $stoplist_hash{ $word }) {
                    if (!exists $lyrics_frequencies[$song_number]{$word}) {
                        $lyrics_frequencies[$song_number]{$word} = 1;
                    } else {
                        $lyrics_frequencies[$song_number]{$word} += 1;
                    }
                }      
            }
        }
        
        $song_num_name_hash{$song_number} = $filename;
        
        $song_number += 1;
        
        close ($fh);
    }   
}

# Reads the input sentence and constructs query vector from it
sub readInputSentence {
    my $input_line = <STDIN>;
    
    my @input_words = split /\s+/, $input_line;
    
    # Construct the stop list hash
    foreach my $word (@input_words)
    {
        $input_sentence_hash{$word} = 1;
    }
}

# Compute similarities of query with 
sub computeSimilarities {

    for my $index (1..$song_number - 1) {
        my $sim = &cosine_sim_a($lyrics_frequencies[$index], \%input_sentence_hash);
	    
	    my $song_name = $song_num_name_hash{$index};
	   
	    # Store the similarities along with the song name
	    $similarity_map{$song_name} = $sim;
	    
	    
    }

}

# Gets the cosine similarity between two vectors
sub cosine_sim_a {

    my $vec1 = shift;
    my $vec2 = shift;

    my $num     = 0;
    my $sum_sq1 = 0;
    my $sum_sq2 = 0;

    my @val1 = values %{ $vec1 };
    my @val2 = values %{ $vec2 };

    # determine shortest length vector. This should speed 
    # things up if one vector is considerable longer than
    # the other (i.e. query vector to document vector).

    if ((scalar @val1) > (scalar @val2)) {
	my $tmp  = $vec1;
	   $vec1 = $vec2;
	   $vec2 = $tmp;
    }

    # calculate the cross product

    my $key = undef;
    my $val = undef;

    while (($key, $val) = each %{ $vec1 }) {
	$num += $val * ($$vec2{ $key } || 0);
    }

    # calculate the sum of squares

    my $term = undef;

    foreach $term (@val1) { $sum_sq1 += $term * $term; }
    foreach $term (@val2) { $sum_sq2 += $term * $term; }

    return ( $num / sqrt( $sum_sq1 * $sum_sq2 ));
}

sub main {
    &readLyricsFiles;
    &readInputSentence;
    &computeSimilarities;
}

&main;
