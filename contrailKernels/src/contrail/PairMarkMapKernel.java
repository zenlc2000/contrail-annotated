package contrail;

//import contrail.PairMarkMapKernelInterface;
import org.trifort.rootbeer.runtime.Kernel;

import java.lang.String;
import java.util.Random;

/**
 * Created by zen on 3/6/15.
 */
public class PairMarkMapKernel implements Kernel, PairMarkMapKernelInterface
{
    public String get_nodetxt()
    {
        return _nodetxt;
    }

    public String get_Fbuddy()
    {
        return _Fbuddy;
    }

    public String get_rBuddy()
    {
        return _rBuddy;
    }

    private final String _nodetxt;
    private final String _Fbuddy;
    private final String _rBuddy;
    private static long randseed;
    private Random rfactory = new Random();

    public int getCompressibleCounter()
    {
        return compressibleCounter;
    }

    private int compressibleCounter;

    // PairMarkMapKernel(){}

    public PairMarkMapKernel(String nodetxt, String str_Fbuddy, String str_Rbuddy, long rSeed)
    {
        _nodetxt = nodetxt;
        _Fbuddy = str_Fbuddy;
        _rBuddy = str_Rbuddy;
        randseed = rSeed;


        compressibleCounter = 0;
    }
    @Override
    public void gpuMethod()
    {
//        TailInfo fbuddy = getBuddy(node, "f");
//        TailInfo rbuddy = getBuddy(node, "r");

        if ((_Fbuddy != null) || (_rBuddy != null))
        {
            String[] nodeStr = _nodetxt.split("\t");//node.getNodeId();
            String nodeId = nodeStr[0];

            compressibleCounter++;
            String compress = null;
            String compressdir = null;
            String compressbdir = null;

            if (isMale(nodeId))
            {
                // Prefer Merging forward
                if (_Fbuddy != null)
                {
                    String[] tmp = _Fbuddy.split(" ");
                    boolean fmale = isMale(tmp[0]);

                }

            }


        }
    }

    public boolean isMale(String nodeid)
    {
        rfactory.setSeed(nodeid.hashCode() ^ randseed);

        double rand = rfactory.nextDouble();

        boolean male = (rand >= .5);

        //System.err.println(nodeid + " " + rand + " " + male);

        return male;
    }

    public static void main(String[] args)
    {
        new PairMarkMapKernel(null, null, null, 0);
    }
}


//if ((fbuddy != null) || (rbuddy != null))
//        {
//        String nodeid = node.getNodeId();
//
//        reporter.incrCounter("Contrail", "compressible", 1);
//
//        String compress = null;
//        String compressdir = null;
//        String compressbdir = null;
//
//        if (isMale(node.getNodeId()))
//        {
//        // Prefer Merging forward
//        if (fbuddy != null)
//        {
//        boolean fmale = isMale(fbuddy.id);
//
//        if (!fmale)
//        {
//        compress = fbuddy.id;
//        compressdir = "f";
//        compressbdir = fbuddy.dir;
//        }
//        }
//
//        if ((compress == null) && (rbuddy != null))
//        {
//        boolean rmale = isMale(rbuddy.id);
//
//        if (!rmale)
//        {
//        compress = rbuddy.id;
//        compressdir = "r";
//        compressbdir = rbuddy.dir;
//        }
//        }
//        } else
//        {
//        if ((rbuddy != null) && (fbuddy != null))
//        {
//        boolean fmale = isMale(fbuddy.id);
//        boolean rmale = isMale(rbuddy.id);
//
//        if (!fmale && !rmale &&
//        (nodeid.compareTo(fbuddy.id) < 0) &&
//        (nodeid.compareTo(rbuddy.id) < 0))
//        {
//        // FFF and I'm the local minimum, go ahead and compress
//        compress = fbuddy.id;
//        compressdir = "f";
//        compressbdir = fbuddy.dir;
//        }
//        } else if (rbuddy == null)
//        {
//        boolean fmale = isMale(fbuddy.id);
//
//        if (!fmale && (nodeid.compareTo(fbuddy.id) < 0))
//        {
//        // Its X*=>FF and I'm the local minimum
//        compress = fbuddy.id;
//        compressdir = "f";
//        compressbdir = fbuddy.dir;
//        }
//        } else if (fbuddy == null)
//        {
//        boolean rmale = isMale(rbuddy.id);
//
//        if (!rmale && (nodeid.compareTo(rbuddy.id) < 0))
//        {
//        // Its FF=>X* and I'm the local minimum
//        compress = rbuddy.id;
//        compressdir = "r";
//        compressbdir = rbuddy.dir;
//        }
//        }
//        }
//
//        if (compress != null)
//        {
//        //print STDERR "compress $nodeid $compress $compressdir $compressbdir\n";
//        reporter.incrCounter("Contrail", "mergestomake", 1);
//
//        //Save that I'm supposed to merge
//        node.setMerge(compressdir);
//
//        // Now tell my ~CD neighbors about my new nodeid
//        String toupdate = Node.flip_dir(compressdir);
//
//        for (String adj : Node.dirs)
//        {
//        String key = toupdate + adj;
//
//        String origadj = Node.flip_dir(adj) + compressdir;
//        String newadj = Node.flip_dir(adj) + compressbdir;
//
//        List<String> edges = node.getEdges(key);
//
//        if (edges != null)
//        {
//        for (String p : edges)
//        {
//        reporter.incrCounter("Contrail", "remoteupdate", 1);
//        output.collect(new Text(p),
//        new Text(Node.UPDATEMSG + "\t" + nodeid + "\t" + origadj + "\t" + compress + "\t" + newadj));
//        }
//        }
//        }
//        }
//        }