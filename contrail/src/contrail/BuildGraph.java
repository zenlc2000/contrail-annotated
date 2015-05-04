package contrail;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.trifort.rootbeer.runtime.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class BuildGraph extends Configured implements Tool
{
	private static final Logger sLogger = Logger.getLogger(BuildGraph.class);

	private static class BuildGraphMapper extends MapReduceBase
			implements Mapper<LongWritable, Text, Text, Text>
	{
		private static int K = 0;
		private static int TRIM5 = 0;
		private static int TRIM3 = 0;
		public static boolean USE_GPU;

		public void configure (JobConf job)
		{
			K = Integer.parseInt (job.get ("K"));
			TRIM5 = Integer.parseInt (job.get ("TRIM5"));
			TRIM3 = Integer.parseInt (job.get ("TRIM3"));
			USE_GPU = Boolean.parseBoolean (job.get ("USE_GPU"));
		}

		public void map (LongWritable lineid, Text nodetxt,
							  OutputCollector< Text, Text > output, Reporter reporter)
				throws IOException
		{
			String[] fields = nodetxt.toString ().split ("\t");
		/*	List<Kernel> m_jobs = new ArrayList<Kernel>();

			char[] output_key1 = null ;
			char[] output_value1 = null;
			char[] output_key2 = null;
			char[] output_value2 = null;*/

			if ( fields.length != 2 )
			{
				//System.err.println("Warning: invalid input: \"" + nodetxt.toString() + "\"");
				reporter.incrCounter ("Contrail", "input_lines_invalid", 1);
				return;
			}


			System.out.println ("BuildGraph: " + lineid.toString () + "\t" + nodetxt.toString ());

			String seq = fields[1].toUpperCase ();

			if ( USE_GPU )
			{
				Kernel job = null;
            int seq_len = seq.length () - K;
				char[][] input_array = new char[seq_len * 4][];
				int k = 0;
            for ( int i = 0; i < seq_len; i += 4 )
            {
               if ( seq.length () <= K )
               {
                  //System.err.println("WARNING: read " + tag + " is too short: " + seq);
                  reporter.incrCounter ("Contrail", "reads_short", 1);
                  return;
               }
               else
               {
                  input_array[k++] = (seq.substring (i, i + K)).toCharArray ();
                  input_array[k++] = (seq.substring (i + 1, i + 1 + K)).toCharArray ();

                  String f = seq.substring (i, i + 1);
						input_array[k++] = (Node.rc (f)).toCharArray ();
                  input_array[k++] = (seq.substring (i + K, i + K + 1)).toCharArray ();

               }
            }

            System.out.println ("input_array length = " + input_array.length);

				char[][] resultKeys = new char[input_array.length * 10][]; // Arbitrary size - find a formula relating K and read size
				char[][] resultValues = new char[input_array.length * 10][];
				Class c = null;
				try
				{
					c = Class.forName ("contrail.BuildGraphMapKernel");
					Constructor< Kernel > ctor = c.getConstructor (int.class, int.class, int.class, String.class, char[][].class, char[][].class, char[][].class);
					job = ctor.newInstance (K, TRIM3, TRIM5, fields[0],input_array, resultKeys, resultValues);

				}
				catch ( ClassNotFoundException e )
				{
					e.printStackTrace ();
				}
				catch ( NoSuchMethodException e)
				{
					e.printStackTrace ();
				}
				catch (InvocationTargetException e)
				{
					e.printStackTrace ();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace ();
				}
				catch (InstantiationException e)
				{
					e.printStackTrace ();
				}

            int size = 2048;
            int sizeBy2 = size / 2;
            //int numMultiProcessors = 14;
            //int blocksPerMultiProcessor = 512;
            int numMultiProcessors = 2;
            int blocksPerMultiProcessor = 64;
            int outerCount = numMultiProcessors*blocksPerMultiProcessor;

				final int threadSize = seq_len; // 32;
				final int blockSize = 512;
				System.out.println ("New Rootbeer");
				Rootbeer rootbeer = new Rootbeer ();
				List< GpuDevice > devices = rootbeer.getDevices ();
				System.out.println ("Devices list");
				GpuDevice device0 = devices.get (0);
				System.out.println ("Device 0");
				Context context0 = device0.createContext ();
				System.out.println ("Created Context");
				context0.setCacheConfig (CacheConfig.PREFER_SHARED);
				System.out.println ("Prefer Shared");
				context0.setThreadConfig (seq_len/2, outerCount, outerCount * sizeBy2);
				System.out.println ("Set ThreadConfig");
				context0.setKernel (job);
				context0.buildState ();
				System.out.println ("BuildState");
				context0.run ();

				for ( int i = 0; i < resultKeys.length; i++ )
				{
					String[] k_fields = (new String (resultKeys[i])).split ("\t");
					String[] v_fields = (new String (resultValues[i])).split ("\t");

					if (k_fields[0] == "r")
					{
						reporter.incrCounter("Contrail", "reads_skipped", Long.parseLong (v_fields[0]));
					}
					else if (k_fields[0] == "s")
					{
						reporter.incrCounter("Contrail", "reads_short", Long.parseLong (v_fields[0]));
					}
					else
					{
						output.collect (new Text(k_fields[1]), new Text(v_fields[1]));
						output.collect (new Text(k_fields[2]), new Text(v_fields[2]));
					}

				}

			}
			else
			{


				String tag = fields[0];

				tag.replaceAll (" ", "_");
				tag.replaceAll (":", "_");
				tag.replaceAll ("#", "_");
				tag.replaceAll ("-", "_");
				tag.replaceAll (".", "_");

//				String seq = fields[1].toUpperCase ();

				// Hard chop a few bases off of each end of the read
				if ( TRIM5 > 0 || TRIM3 > 0 )
				{
					// System.err.println("orig: " + seq);
					seq = seq.substring (TRIM5, seq.length () - TRIM5 - TRIM3);
					// System.err.println("trim: " + seq);
				}

				// Automatically trim Ns off the very ends of reads
				int endn = 0;
				while ( endn < seq.length () && seq.charAt (seq.length () - 1 - endn) == 'N' ) { endn++; }
				if ( endn > 0 ) { seq = seq.substring (0, seq.length () - endn); }

				int startn = 0;
				while ( startn < seq.length () && seq.charAt (startn) == 'N' ) { startn++; }
				if ( startn > 0 ) { seq = seq.substring (startn, seq.length () - startn); }

				// Check for non-dna characters
				if ( seq.matches (".*[^ACGT].*") )
				{
					//System.err.println("WARNING: non-DNA characters found in " + tag + ": " + seq);
					reporter.incrCounter ("Contrail", "reads_skipped", 1);
					return;
				}

				// check for short reads
				if ( seq.length () <= K )
				{
					//System.err.println("WARNING: read " + tag + " is too short: " + seq);
					reporter.incrCounter ("Contrail", "reads_short", 1);
					return;
				}

				// Now emit the edges of the de Bruijn Graph

				char ustate = '5';
				char vstate = 'i';

				Set< String > seenmers = new HashSet< String > ();

				String chunkstr = "";
				int chunk = 0;

				int end = seq.length () - K;
				String[] seen_mers = seenmers.toArray (new String[seenmers.size ()]);


				for ( int i = 0; i < end; i++ )
				{
					String u = seq.substring (i, i + K);
					String v = seq.substring (i + 1, i + 1 + K);

					String f = seq.substring (i, i + 1);
					String l = seq.substring (i + K, i + K + 1);
					f = Node.rc (f);

					char ud = Node.canonicaldir (u);
					char vd = Node.canonicaldir (v);

					String t = Character.toString (ud) + vd;
					String tr = Node.flip_link (t);

					String uc0 = Node.canonicalseq (u);
					String vc0 = Node.canonicalseq (v);

					String uc = Node.str2dna (uc0);
					String vc = Node.str2dna (vc0);

					System.out.println (u + " " + uc0 + " " + ud + " " + uc);
					System.out.println (v + " " + vc0 + " " + vd + " " + vc);

					if ( (i == 0) && (ud == 'r') )
					{
						ustate = '6';
					}
					if ( i + 1 == end )
					{
						vstate = '3';
					}

					boolean seen = (seenmers.contains (u) || seenmers.contains (v) || u.equals (v));
					seenmers.add (u);

					if ( seen )
					{
						chunk++;
						chunkstr = "c" + chunk;
						//#print STDERR "repeat internal to $tag: $uc u$i $chunk\n";
					}

					//System.out.println(uc + "\t" + t + "\t" + l + "\t" + tag + chunkstr + "\t" + ustate);

					output.collect (new Text (uc),
							new Text (t + "\t" + l + "\t" + tag + chunkstr + "\t" + ustate));

					if ( seen )
					{
						chunk++;
						chunkstr = "c" + chunk;
						//#print STDERR "repeat internal to $tag: $vc v$i $chunk\n";
					}

					//print "$vc\t$tr\t$f\t$tag$chunk\t$vstate\n";

					System.out.println (vc + "\t" + tr + "\t" + f + "\t" + tag + chunkstr + "\t" + vstate);

					output.collect (new Text (vc),
							new Text (tr + "\t" + f + "\t" + tag + chunkstr + "\t" + vstate));

					ustate = 'm';
				}

				reporter.incrCounter ("Contrail", "reads_good", 1);
				reporter.incrCounter ("Contrail", "reads_goodbp", seq.length ());
			}
		}
	}

	private static class BuildGraphReducer extends MapReduceBase
			implements Reducer<Text, Text, Text, Text>
	{
		private static int K = 0;
		private static int MAXTHREADREADS = 0;
		private static int MAXR5 = 0;
		private static boolean RECORD_ALL_THREADS = false;

		public void configure(JobConf job)
		{
			K = Integer.parseInt(job.get("K"));
			MAXTHREADREADS = Integer.parseInt(job.get("MAXTHREADREADS"));
			MAXR5 = Integer.parseInt(job.get("MAXR5"));
			RECORD_ALL_THREADS = Integer.parseInt(job.get("RECORD_ALL_THREADS")) == 1;
		}

		public void reduce(Text curnode, Iterator<Text> iter,
								 OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException
		{
			Node node = new Node();

			String mertag = null;
			float cov = 0;

			// Hash keyed based on "f" or "r" direction of the edge
			// Contains nested hash that is keyed based on single-base extension of the k-mer (neighboring k-mer)
			// and stacks upon a list the read ids that contain that neighboring k-mer

			//  storage the de Bruijn in the hash below
			//  {forward / reverse} -> [ { A / C / G / T } -> [read_id1, read_id2, ...]  ]
			Map<String, Map<String, List<String>>> edges = new HashMap<String, Map<String, List<String>>>();

			while(iter.hasNext())
			{
				String valstr = iter.next().toString();
				String [] vals = valstr.split("\t");

				String type     = vals[0]; // edge type between mers
				String neighbor = vals[1]; // id of neighboring node
				String tag      = vals[2]; // id of read contributing to edge
				String state    = vals[3]; // internal or end mer

				// Add the edge to the neighbor
				Map<String, List<String>> neighborinfo = null;
				if (edges.containsKey(type))
				{
					neighborinfo = edges.get(type);
				}
				else
				{
					neighborinfo = new HashMap<String, List<String>>();
					edges.put(type, neighborinfo);
				}


				// Now record the read supports the edge
				List<String> tags = null;
				if (neighborinfo.containsKey(neighbor))
				{
					tags = neighborinfo.get(neighbor);
				}
				else
				{
					tags = new ArrayList<String>();
					neighborinfo.put(neighbor, tags);
				}

				if (tags.size() < MAXTHREADREADS)
				{
					tags.add(tag);
				}

				// Check on the mertag
				if (mertag == null || (tag.compareTo(mertag) < 0))
				{
					mertag = tag;
				}

				// Update coverage, offsets
				if (!state.equals("i"))
				{
					cov++;

					if (state.equals("6"))
					{
						node.addR5(tag, K-1, 1, MAXR5);
					}
					else if (state.equals("5"))
					{
						node.addR5(tag, 0, 0, MAXR5);
					}
				}
			}

			node.setMertag(mertag);
			node.setCoverage(cov);

			String seq = Node.dna2str(curnode.toString());
			String rc  = Node.rc(seq);

			node.setstr_raw(curnode.toString());

			seq = seq.substring(1);
			rc  = rc.substring(1);

			char [] dirs = {'f', 'r'};

			for (int d = 0; d < 2; d++)
			{
				String x = Character.toString(dirs[d]);

				int degree = 0;

				for (int e = 0; e < 2; e++)
				{
					String t = x + dirs[e];

					if (edges.containsKey(t))
					{
						degree += edges.get(t).size();
					}
				}

				for(int e = 0; e < 2; e++)
				{
					String t = x + dirs[e];

					if (edges.containsKey(t))
					{
						Map<String, List<String>> edgeinfo = edges.get(t);

						for (String vc : edgeinfo.keySet())
						{
							String v = seq;
							if (dirs[d] == 'r') { v = rc; }

							v = v +  vc;

							if (dirs[e] == 'r') { v = Node.rc(v); }

							String link = Node.str2dna(v);

							node.addEdge(t, link);

							if ((degree > 1) || RECORD_ALL_THREADS)
							{
								for (String r : edgeinfo.get(vc))
								{
									node.addThread(t, link, r);
								}
							}
						}
					}
				}
			}

			output.collect(curnode, new Text(node.toNodeMsg()));
			reporter.incrCounter("Contrail", "nodecount", 1);
		}
	}



	public RunningJob run(String inputPath, String outputPath) throws Exception
	{
		sLogger.info("Tool name: BuildGraph");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);

		JobConf conf = new JobConf(Stats.class);
		conf.setJobName("BuildGraph " + inputPath + " " + ContrailConfig.K);

		ContrailConfig.initializeConfiguration(conf);

		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(BuildGraphMapper.class);
		conf.setReducerClass(BuildGraphReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}

	public int run(String[] args) throws Exception
	{
		String inputPath  = "/Users/mschatz/build/Contrail/data/B.anthracis.36.50k.sfa";
		String outputPath = "/users/mschatz/try/build";
		ContrailConfig.K = 21;

		long starttime = System.currentTimeMillis();

		run(inputPath, outputPath);

		long endtime = System.currentTimeMillis();

		float diff = (float) (((float) (endtime - starttime)) / 1000.0);

		System.out.println("Runtime: " + diff + " s");

		return 0;
	}

	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run (new Configuration (), new BuildGraph (), args);
		System.exit(res);
	}
}
