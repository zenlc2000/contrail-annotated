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
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class Compressible extends Configured implements Tool
{
   private static final Logger sLogger = Logger.getLogger (Compressible.class);

   private static class CompressibleMapper extends MapReduceBase
         implements Mapper< LongWritable, Text, Text, Text >
   {
      public static boolean USE_GPU;

      public void configure (JobConf job)
      {
         USE_GPU = Boolean.parseBoolean (job.get ("USE_GPU"));
      }

      public void map (LongWritable lineid, Text nodetxt,
                       OutputCollector< Text, Text > output, Reporter reporter)
            throws IOException
      {
         Node node = new Node ();
         node.fromNodeMsg (nodetxt.toString ());
         Rootbeer rootbeer = new Rootbeer ();
         List< Kernel > m_jobs = new ArrayList< Kernel > ();
         for ( String adj : Node.dirs )
         {
            if ( !USE_GPU )
            {
               try
               {
                  Class c = Class.forName ("contrail.BuildGraphMapKernel");
                  Constructor< Kernel > ctor = c.getConstructor (String.class);
                  Kernel job = ctor.newInstance (nodetxt.toString ());
                  m_jobs.add (job);
               }
               catch ( ClassNotFoundException e )
               {
                  e.printStackTrace ();
               }
               catch ( NoSuchMethodException e )
               {
                  e.printStackTrace ();
               }
               catch ( InvocationTargetException e )
               {
                  e.printStackTrace ();
               }
               catch ( InstantiationException e )
               {
                  e.printStackTrace ();
               }
               catch ( IllegalAccessException e )
               {
                  e.printStackTrace ();
               }


            }
            else
            {
               node.setCanCompress (adj, false);

               TailInfo next = node.gettail (adj);

               if ( next != null )
               {
                  if ( next.id.equals (node.getNodeId ()) )
                  {
                     continue;
                  }

                  reporter.incrCounter ("Contrail", "remotemark", 1);

                  output.collect (new Text (next.id),
                        new Text (Node.HASUNIQUEP + "\t" + node.getNodeId () + "\t" + adj));
               }
            }
         }

         output.collect (new Text (node.getNodeId ()), new Text (node.toNodeMsg ()));

         reporter.incrCounter ("Contrail", "nodes", 1);
      }
   }

   private static class CompressibleReducer extends MapReduceBase
         implements Reducer< Text, Text, Text, Text >
   {
      public void reduce (Text key, Iterator< Text > iter,
                          OutputCollector< Text, Text > output, Reporter reporter)
            throws IOException
      {
         Node node = new Node (key.toString ());

         Set< String > f_unique = new HashSet< String > ();
         Set< String > r_unique = new HashSet< String > ();

         int sawnode = 0;

         while ( iter.hasNext () )
         {
            String msg = iter.next ().toString ();

            //System.err.println(key.toString() + "\t" + msg);

            String[] vals = msg.split ("\t");

            if ( vals[0].equals (Node.NODEMSG) )
            {
               node.parseNodeMsg (vals, 0);
               sawnode++;
            }
            else if ( vals[0].equals (Node.HASUNIQUEP) )
            {
               if ( vals[2].equals ("f") ) { f_unique.add (vals[1]); }
               else if ( vals[2].equals ("r") ) { r_unique.add (vals[1]); }
            }
            else
            {
               throw new IOException ("Unknown msgtype: " + msg);
            }
         }

         if ( sawnode != 1 )
         {
            throw new IOException ("ERROR: Didn't see exactly 1 nodemsg (" + sawnode + ") for " + key.toString ());
         }

         for ( String adj : Node.dirs )
         {
            TailInfo next = node.gettail (adj);

            if ( next != null )
            {
               if ( (next.dir.equals ("f") && r_unique.contains (next.id)) ||
                     (next.dir.equals ("r") && f_unique.contains (next.id)) )
               {
                  node.setCanCompress (adj, true);
                  reporter.incrCounter ("Contrail", "compressible", 1);
               }
            }
         }

         output.collect (new Text (node.getNodeId ()), new Text (node.toNodeMsg ()));
      }
   }


   public RunningJob run (String inputPath, String outputPath) throws Exception
   {
      sLogger.info ("Tool name: Compressible");
      sLogger.info (" - input: " + inputPath);
      sLogger.info (" - output: " + outputPath);

      JobConf conf = new JobConf (Stats.class);
      conf.setJobName ("Compressible " + inputPath);

      ContrailConfig.initializeConfiguration (conf);

      FileInputFormat.addInputPath (conf, new Path (inputPath));
      FileOutputFormat.setOutputPath (conf, new Path (outputPath));

      conf.setInputFormat (TextInputFormat.class);
      conf.setOutputFormat (TextOutputFormat.class);

      conf.setMapOutputKeyClass (Text.class);
      conf.setMapOutputValueClass (Text.class);

      conf.setOutputKeyClass (Text.class);
      conf.setOutputValueClass (Text.class);

      conf.setMapperClass (CompressibleMapper.class);
      conf.setReducerClass (CompressibleReducer.class);

      //delete the output directory if it exists already
      FileSystem.get (conf).delete (new Path (outputPath), true);

      return JobClient.runJob (conf);
   }


   public int run (String[] args) throws Exception
   {
      String inputPath = "/Users/mschatz/try/quickmerge";
      String outputPath = "/users/mschatz/try/compressible";

      run (inputPath, outputPath);

      return 0;
   }

   public static void main (String[] args) throws Exception
   {
      int res = ToolRunner.run (new Configuration (), new Compressible (), args);
      System.exit (res);
   }
}