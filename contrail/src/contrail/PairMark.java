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
import org.trifort.rootbeer.runtime.Kernel;
import org.trifort.rootbeer.runtime.Rootbeer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class PairMark extends Configured implements Tool 
{	
	private static final Logger sLogger = Logger.getLogger(PairMark.class);
	
	private static class PairMarkMapper extends MapReduceBase 
    implements Mapper<LongWritable, Text, Text, Text> 
	{
		private static long randseed = 0;
		private Random rfactory = new Random();

        public static boolean USE_GPU;
		
		public void configure(JobConf job) 
		{
			randseed = Long.parseLong(job.get("randseed"));
            USE_GPU = Boolean.parseBoolean(job.get("USE_GPU"));
		}
		
		public boolean isMale(String nodeid)
		{
			rfactory.setSeed(nodeid.hashCode() ^ randseed);
			
			double rand = rfactory.nextDouble();
			
			boolean male = (rand >= .5);
			
			//System.err.println(nodeid + " " + rand + " " + male);

			return male;
		}
		
		public TailInfo getBuddy(Node node, String dir)
		{
			if (node.canCompress(dir))
			{
				return node.gettail(dir);
			}
			
			return null;
		}
		
		public void map(LongWritable lineid, Text nodetxt,
                OutputCollector<Text, Text> output, Reporter reporter)
                throws IOException 
        {


            List<Kernel> m_jobs = new ArrayList<Kernel>();
			Node node = new Node();
			node.fromNodeMsg(nodetxt.toString());

			TailInfo fbuddy = getBuddy(node, "f");
			TailInfo rbuddy = getBuddy(node, "r");

            if (USE_GPU)
            {
                sLogger.info("----------> Starting PairMark Mapper GPU <----------");


                String str_Fbuddy = fbuddy.toString();
                String str_Rbuddy = rbuddy.toString();

                try
                {
                    Class c = Class.forName("Contrail.PairMarkMapKernel");
                    Constructor<Kernel> ctor = c.getConstructor(String.class, String.class, String.class, Long.class);
                    Kernel job = ctor.newInstance(nodetxt, str_Fbuddy, str_Rbuddy, randseed);
                    m_jobs.add(job);
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }

                Rootbeer rootbeer = new Rootbeer();
                rootbeer.run(m_jobs);       // Run on GPU

                // TODO: Write code to get data back and translate to output
            }
else
            {
                if ((fbuddy != null) || (rbuddy != null))
                {
                    String nodeid = node.getNodeId();

                    reporter.incrCounter("Contrail", "compressible", 1);

                    String compress = null;
                    String compressdir = null;
                    String compressbdir = null;

                    if (isMale(node.getNodeId()))
                    {
                        // Prefer Merging forward
                        if (fbuddy != null)
                        {
                            boolean fmale = isMale(fbuddy.id);

                            if (!fmale)
                            {
                                compress = fbuddy.id;
                                compressdir = "f";
                                compressbdir = fbuddy.dir;
                            }
                        }

                        if ((compress == null) && (rbuddy != null))
                        {
                            boolean rmale = isMale(rbuddy.id);

                            if (!rmale)
                            {
                                compress = rbuddy.id;
                                compressdir = "r";
                                compressbdir = rbuddy.dir;
                            }
                        }
                    } else
                    {
                        if ((rbuddy != null) && (fbuddy != null))
                        {
                            boolean fmale = isMale(fbuddy.id);
                            boolean rmale = isMale(rbuddy.id);

                            if (!fmale && !rmale &&
                                    (nodeid.compareTo(fbuddy.id) < 0) &&
                                    (nodeid.compareTo(rbuddy.id) < 0))
                            {
                                // FFF and I'm the local minimum, go ahead and compress
                                compress = fbuddy.id;
                                compressdir = "f";
                                compressbdir = fbuddy.dir;
                            }
                        } else if (rbuddy == null)
                        {
                            boolean fmale = isMale(fbuddy.id);

                            if (!fmale && (nodeid.compareTo(fbuddy.id) < 0))
                            {
                                // Its X*=>FF and I'm the local minimum
                                compress = fbuddy.id;
                                compressdir = "f";
                                compressbdir = fbuddy.dir;
                            }
                        } else if (fbuddy == null)
                        {
                            boolean rmale = isMale(rbuddy.id);

                            if (!rmale && (nodeid.compareTo(rbuddy.id) < 0))
                            {
                                // Its FF=>X* and I'm the local minimum
                                compress = rbuddy.id;
                                compressdir = "r";
                                compressbdir = rbuddy.dir;
                            }
                        }
                    }

                    if (compress != null)
                    {
                        //print STDERR "compress $nodeid $compress $compressdir $compressbdir\n";
                        reporter.incrCounter("Contrail", "mergestomake", 1);

                        //Save that I'm supposed to merge
                        node.setMerge(compressdir);

                        // Now tell my ~CD neighbors about my new nodeid
                        String toupdate = Node.flip_dir(compressdir);

                        for (String adj : Node.dirs)
                        {
                            String key = toupdate + adj;

                            String origadj = Node.flip_dir(adj) + compressdir;
                            String newadj = Node.flip_dir(adj) + compressbdir;

                            List<String> edges = node.getEdges(key);

                            if (edges != null)
                            {
                                for (String p : edges)
                                {
                                    reporter.incrCounter("Contrail", "remoteupdate", 1);
                                    output.collect(new Text(p),
                                            new Text(Node.UPDATEMSG + "\t" + nodeid + "\t" + origadj + "\t" + compress + "\t" + newadj));
                                }
                            }
                        }
                    }
                }
            }
			output.collect(new Text(node.getNodeId()), new Text(node.toNodeMsg()));
			reporter.incrCounter("Contrail", "nodes", 1);	
        }
	}
	
	private static class PairMarkReducer extends MapReduceBase 
	implements Reducer<Text, Text, Text, Text> 
	{
		private static long randseed = 0;
		
		public void configure(JobConf job) {
			randseed = Long.parseLong(job.get("randseed"));
		}
		
		private class Update
		{
			public String oid;
			public String odir;
			public String nid;
			public String ndir;
		}
		
		public void reduce(Text nodeid, Iterator<Text> iter,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException 
		{
			Node node = new Node(nodeid.toString());
			List<Update> updates = new ArrayList<Update>();
			
			int sawnode = 0;
			
			while(iter.hasNext())
			{
				String msg = iter.next().toString();
				
				//System.err.println(key.toString() + "\t" + msg);
				
				String [] vals = msg.split("\t");
				
				if (vals[0].equals(Node.NODEMSG))
				{
					node.parseNodeMsg(vals, 0);
					sawnode++;
				}
				else if (vals[0].equals(Node.UPDATEMSG))
				{
					Update up = new Update();
					
					up.oid  = vals[1];
					up.odir = vals[2];
					up.nid  = vals[3];
					up.ndir = vals[4];
					
					updates.add(up);
				}
				else
				{
					throw new IOException("Unknown msgtype: " + msg);
				}
			}
			
			if (sawnode != 1)
			{
				throw new IOException("ERROR: Didn't see exactly 1 nodemsg (" + sawnode + ") for " + nodeid.toString());
			}
			
			if (updates.size() > 0)
			{
				for(Update up : updates)
				{
					node.replacelink(up.oid, up.odir, up.nid, up.ndir);
				}
			}
			
			output.collect(nodeid, new Text(node.toNodeMsg()));
		}
	}

	
	public RunningJob run(String inputPath, String outputPath, long randseed) throws Exception
	{ 
		sLogger.info("Tool name: PairMark");
		sLogger.info(" - input: "  + inputPath);
		sLogger.info(" - output: " + outputPath);
		sLogger.info(" - randseed: " + randseed);
		
		JobConf conf = new JobConf(Stats.class);
		conf.setJobName("PairMark " + inputPath);
		
		ContrailConfig.initializeConfiguration(conf);
		conf.setLong("randseed", randseed);
			
		FileInputFormat.addInputPath(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(PairMarkMapper.class);
		conf.setReducerClass(PairMarkReducer.class);

		//delete the output directory if it exists already
		FileSystem.get(conf).delete(new Path(outputPath), true);

		return JobClient.runJob(conf);
	}
	
	
	public int run(String[] args) throws Exception 
	{
		String inputPath  = "/Users/mschatz/try/compressible/";
		String outputPath = "/users/mschatz/try/mark1/";
		long randseed = 123456789;
		
		run(inputPath, outputPath, randseed);
		
		return 0;
	}

	public static void main(String[] args) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new PairMark(), args);
		System.exit(res);
	}
}
