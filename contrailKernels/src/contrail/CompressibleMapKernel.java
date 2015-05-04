package contrail;

import org.trifort.rootbeer.runtime.Kernel;

import java.io.IOException;

/**
 * Created by zen on 4/7/15.
 */
public class CompressibleMapKernel implements Kernel, CompressibleMapInterface
{

    static String [] dirs      = {"f", "r"};
    static String node;
    int remotemark = 0;
    char[] output_key = null;
    String nodeId;

    public CompressibleMapKernel( String n )
    {
        this.node = n;

    }

    public void fromNodeMsg(String nodestr) throws IOException
    {
//        fields.clear();

        String [] items = nodestr.split("\t");

        nodeId = items[0];
//        parseNodeMsg(items, 1);
    }

    @Override
    public void gpuMethod()
    {
 /*       node.setCanCompress(adj, false);

        TailInfo next = node.gettail(adj);

        if (next != null)
        {
            if (next.id.equals(node.getNodeId()))
            {
                continue; // original intent is: if next.id == node.id then skip counter and output
                        // maybe reverse logic... do counter and output of !=
            }

//            reporter.incrCounter("Contrail", "remotemark", 1);
                remotemark++;
            //            output.collect(new Text(next.id),
            output_key = nodeId.toCharArray ();
//                    new Text(Node.HASUNIQUEP + "\t" + node.getNodeId() + "\t" + adj));
        }*/
    }

    public static void main( String[] args )
    {
        new CompressibleMapKernel( null );
    }
}
