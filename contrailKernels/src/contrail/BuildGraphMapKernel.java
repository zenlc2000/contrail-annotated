package contrail;

import org.trifort.rootbeer.runtime.Kernel;
import org.trifort.rootbeer.runtime.RootbeerGpu;

import java.util.Arrays;

/**
 * Created by zen on 3/13/15.
 */
public class BuildGraphMapKernel implements Kernel, BuildGraphMapKernelInterface
{
    //    String seq;
    public int uIdx,vIdx, oIdx;
    char[] seq;
    int i;
    int K;
    String tag;
    //char[] tag;
    public static String[] seenmers;

    public char[] output_key1;
    public char[] output_value1;
    public char[] output_key2;
    public char[] output_value2;

    public char ustate;
    public char vstate;

    public char[] chunkstr = null;
    public int chunk = 0;

    //    static String[] dnachars = { "A", "C", "G", "T" };
    public static String[] str2dnaKeys = {"A", "AA", "AC", "AG", "AT", "C", "CA", "CC", "CG", "CT", "G", "GA", "GC", "GG", "GT", "T", "TA", "TC", "TG", "TT", "AAA",
            "AAAA", "AAAC", "AAAG", "AAAT", "AAC", "AACA", "AACC", "AACG", "AACT", "AAG", "AAGA", "AAGC", "AAGG", "AAGT", "AAT", "AATA",
            "AATC", "AATG", "AATT", "ACA", "ACAA", "ACAC", "ACAG", "ACAT", "ACC", "ACCA", "ACCC", "ACCG", "ACCT", "ACG", "ACGA", "ACGC",
            "ACGG", "ACGT", "ACT", "ACTA", "ACTC", "ACTG", "ACTT", "AGA", "AGAA", "AGAC", "AGAG", "AGAT", "AGC", "AGCA", "AGCC", "AGCG",
            "AGCT", "AGG", "AGGA", "AGGC", "AGGG", "AGGT", "AGT", "AGTA", "AGTC", "AGTG", "AGTT", "ATA", "ATAA", "ATAC", "ATAG", "ATAT",
            "ATC", "ATCA", "ATCC", "ATCG", "ATCT", "ATG", "ATGA", "ATGC", "ATGG", "ATGT", "ATT", "ATTA", "ATTC", "ATTG", "ATTT", "CAA",
            "CAAA", "CAAC", "CAAG", "CAAT", "CAC", "CACA", "CACC", "CACG", "CACT", "CAG", "CAGA", "CAGC", "CAGG", "CAGT", "CAT", "CATA",
            "CATC", "CATG", "CATT", "CCA", "CCAA", "CCAC", "CCAG", "CCAT", "CCC", "CCCA", "CCCC", "CCCG", "CCCT", "CCG", "CCGA", "CCGC",
            "CCGG", "CCGT", "CCT", "CCTA", "CCTC", "CCTG", "CCTT", "CGA", "CGAA", "CGAC", "CGAG", "CGAT", "CGC", "CGCA", "CGCC", "CGCG",
            "CGCT", "CGG", "CGGA", "CGGC", "CGGG", "CGGT", "CGT", "CGTA", "CGTC", "CGTG", "CGTT", "CTA", "CTAA", "CTAC", "CTAG", "CTAT",
            "CTC", "CTCA", "CTCC", "CTCG", "CTCT", "CTG", "CTGA", "CTGC", "CTGG", "CTGT", "CTT", "CTTA", "CTTC", "CTTG", "CTTT", "GAA",
            "GAAA", "GAAC", "GAAG", "GAAT", "GAC", "GACA", "GACC", "GACG", "GACT", "GAG", "GAGA", "GAGC", "GAGG", "GAGT", "GAT", "GATA",
            "GATC", "GATG", "GATT", "GCA", "GCAA", "GCAC", "GCAG", "GCAT", "GCC", "GCCA", "GCCC", "GCCG", "GCCT", "GCG", "GCGA", "GCGC",
            "GCGG", "GCGT", "GCT", "GCTA", "GCTC", "GCTG", "GCTT", "GGA", "GGAA", "GGAC", "GGAG", "GGAT", "GGC", "GGCA", "GGCC", "GGCG",
            "GGCT", "GGG", "GGGA", "GGGC", "GGGG", "GGGT", "GGT", "GGTA", "GGTC", "GGTG", "GGTT", "GTA", "GTAA", "GTAC", "GTAG", "GTAT",
            "GTC", "GTCA", "GTCC", "GTCG", "GTCT", "GTG", "GTGA", "GTGC", "GTGG", "GTGT", "GTT", "GTTA", "GTTC", "GTTG", "GTTT", "TAA",
            "TAAA", "TAAC", "TAAG", "TAAT", "TAC", "TACA", "TACC", "TACG", "TACT", "TAG", "TAGA", "TAGC", "TAGG", "TAGT", "TAT", "TATA",
            "TATC", "TATG", "TATT", "TCA", "TCAA", "TCAC", "TCAG", "TCAT", "TCC", "TCCA", "TCCC", "TCCG", "TCCT", "TCG", "TCGA", "TCGC",
            "TCGG", "TCGT", "TCT", "TCTA", "TCTC", "TCTG", "TCTT", "TGA", "TGAA", "TGAC", "TGAG", "TGAT", "TGC", "TGCA", "TGCC", "TGCG",
            "TGCT", "TGG", "TGGA", "TGGC", "TGGG", "TGGT", "TGT", "TGTA", "TGTC", "TGTG", "TGTT", "TTA", "TTAA", "TTAC", "TTAG", "TTAT",
            "TTC", "TTCA", "TTCC", "TTCG", "TTCT", "TTG", "TTGA", "TTGC", "TTGG", "TTGT", "TTT", "TTTA", "TTTC", "TTTG", "TTTT"};

    public static String[] str2dnaValues = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "BA",
            "BB", "BC", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BK", "BL", "BM", "BN", "BO", "BP", "BQ", "BR", "BS", "BT", "CA", "CB", "CC",
            "CD", "CE", "CF", "CG", "CH", "CI", "CJ", "CK", "CL", "CM", "CN", "CO", "CP", "CQ", "CR", "CS", "CT", "DA", "DB", "DC", "DD", "DE",
            "DF", "DG", "DH", "DI", "DJ", "DK", "DL", "DM", "DN", "DO", "DP", "DQ", "DR", "DS", "DT", "EA", "EB", "EC", "ED", "EE", "EF", "EG",
            "EH", "EI", "EJ", "EK", "EL", "EM", "EN", "EO", "EP", "EQ", "ER", "ES", "ET", "GA", "GB", "GC", "GD", "GE", "GF", "GG", "GH", "GI",
            "GJ", "GK", "GL", "GM", "GN", "GO", "GP", "GQ", "GR", "GS", "GT", "HA", "HB", "HC", "HD", "HE", "HF", "HG", "HH", "HI", "HJ", "HK", "HL",
            "HM", "HN", "HO", "HP", "HQ", "HR", "HS", "HT", "IA", "IB", "IC", "ID", "IE", "IF", "IG", "IH", "II", "IJ", "IK", "IL", "IM", "IN", "IO",
            "IP", "IQ", "IR", "IS", "IT", "JA", "JB", "JC", "JD", "JE", "JF", "JG", "JH", "JI", "JJ", "JK", "JL", "JM", "JN", "JO", "JP", "JQ", "JR",
            "JS", "JT", "LA", "LB", "LC", "LD", "LE", "LF", "LG", "LH", "LI", "LJ", "LK", "LL", "LM", "LN", "LO", "LP", "LQ", "LR", "LS", "LT", "MA",
            "MB", "MC", "MD", "ME", "MF", "MG", "MH", "MI", "MJ", "MK", "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "NA", "NB", "NC", "ND",
            "NE", "NF", "NG", "NH", "NI", "NJ", "NK", "NL", "NM", "NN", "NO", "NP", "NQ", "NR", "NS", "NT", "OA", "OB", "OC", "OD", "OE", "OF", "OG",
            "OH", "OI", "OJ", "OK", "OL", "OM", "ON", "OO", "OP", "OQ", "OR", "OS", "OT", "QA", "QB", "QC", "QD", "QE", "QF", "QG", "QH", "QI", "QJ",
            "QK", "QL", "QM", "QN", "QO", "QP", "QQ", "QR", "QS", "QT", "RA", "RB", "RC", "RD", "RE", "RF", "RG", "RH", "RI", "RJ", "RK", "RL", "RM",
            "RN", "RO", "RP", "RQ", "RR", "RS", "RT", "SA", "SB", "SC", "SD", "SE", "SF", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SP",
            "SQ", "SR", "SS", "ST", "TA", "TB", "TC", "TD", "TE", "TF", "TG", "TH", "TI", "TJ", "TK", "TL", "TM", "TN", "TO", "TP", "TQ", "TR", "TS", "TT"};


    public char[] getOutput_key1()
    {
        return output_key1;
    }

    public char[] getOutput_value1()
    {
        return output_value1;
    }

    public char[] getOutput_key2()
    {
        return output_key2;
    }

    public char[] getOutput_value2()
    {
        return output_value2;
    }

    public String[] getSeenmers()
    {
        return this.seenmers;
    }

    public BuildGraphMapKernel()
    {
    }

    public BuildGraphMapKernel(char[] sequence, int kval, int index, String[] seen_mers, String t, char[] output_key1,
                               char[] output_value1, char[] output_key2, char[] output_value2, char ustate, char vstate, int chunk, String chunkstr)
    {
        this.seq = sequence;
        this.i = index;
        this.K = kval;
        this.seenmers = seen_mers;
        this.tag = t;
        this.output_key1 = output_key1;
        this.output_value1 = output_value1;
        this.output_key2 = output_key2;
        this.output_value2 = output_value2;

//        uIdx = RootbeerGpu.getBlockIdxx();
//        vIdx = uIdx + 4; // size of int
//        oIdx = vIdx + 4;

//        char ch = RootbeerGpu.getSharedChar(uIdx);
//        char vch = RootbeerGpu.getSharedChar(vIdx);

//        this.ustate = ustate;

//        if (ustate == '5')
//            this.ustate = ustate;
//        else
        if (i == 0)
            this.ustate = '5';//RootbeerGpu.getSharedChar(0);
        else
            this.ustate = 'm';

//        if (vch != 'i' || vch != '3')
            this.vstate = vstate;
//        else
//            this.vstate = vch;

        try
        {
            Object[] tmp = (Object[]) RootbeerGpu.getSharedObject(oIdx);
            seenmers = (String[]) tmp[0];
        }
        catch (NullPointerException e)
        {
            seenmers = seen_mers;
        }
//        this.vstate = vstate;
        this.chunk = chunk;
        this.chunkstr = chunkstr.toCharArray();

    }

    public static char[] flip_link(char[] link)
    {
        if (link[0] == 'f' && link[1] == 'f')
        {
            char[] flink = {'r', 'r'};
            return flink;
        }

        if (link[0] == 'f' && link[1] == 'r')
        {
            char[] flink = {'f', 'r'};
            return flink;
        } //{ return "fr"; }
        if (link[0] == 'r' && link[1] == 'f')
        {
            char[] flink = {'r', 'f'};
            return flink;
        }//{ return "rf"; }
        if (link[0] == 'r' && link[1] == 'r')
        {
            char[] flink = {'f', 'f'};
            return flink;
        }//{ return "ff"; }
        else
        {
            link = null;
        }

        // throw new IOException("Unknown link type: " + link);
        return link;
    }


    public static char[] toCharArray(String str)
    {
        char[] ch = new char[str.length()];
        for (int i = 0; i < str.length(); i++)
        {
            ch[i] = str.charAt(i);
        }

        return ch;
    }


    public static int compareTo(char[] seq, char[] rc)
    {
        for (int i = 0; i < seq.length; i++)
        {
            if (seq[i] != rc[i])
            {
                return seq[i] - rc[i];
            }
        }

        return 0;
    }

    public static char canonicaldir(char[] seq)
    {
        char[] rc = rc(seq);

        if (compareTo(seq, rc) < 0)
        {
            return 'f';
        }

        return 'r';
    }

    public static char[] canonicalseq(char[] seq)
    {
        char[] ch_ret = rc(seq);
        if (compareTo(seq, ch_ret) < 0)
        {
            return seq;
        }

        return ch_ret;
    }


    public static char[] rc(char[] seq)
    {
        char[] sb = new char[seq.length];
        int k = 0;

        for (int i = seq.length - 1; i >= 0; i--)
        {
            switch(seq[i])
            {
                case 'A' :  sb[ k ] = 'T'; break;
                case 'T' :  sb[ k ] = 'A'; break;
                case 'C' :  sb[ k ] = 'G'; break;
                case 'G' :  sb[ k ] = 'C'; break;
            }
//            if (seq[i] == 'A')
//            {
//                sb[k] = 'T';
//            } else if (seq[i] == 'T')
//            {
//                sb[k] = 'A';
//            } else if (seq[i] == 'C')
//            {
//                sb[k] = 'G';
//            } else if (seq[i] == 'G')
//            {
//                sb[k] = 'C';
//            }
            k++;
        }

        return sb;
    }


    public static boolean contains(String[] arr, char[] targetValue)
    {
        char[] str;
        for (String s : arr)
        {
            if (s != null)
            {
                str = toCharArray(s);
                if (targetValue.length > str.length)
                {
                    return false;
                } else if (str.length == targetValue.length)
                {
                    if (compareTo(str, targetValue) == 0)
                    {
                        return true;
                    }
                } else
                {
                    int diff = str.length - targetValue.length;
                    for (int i = 0; i < diff; i++)
                    {
                        if (compareTo(substring(str, i, i + targetValue.length), targetValue) == 0)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void add(char[] u)
    {
        String tmp = "" + u;
        for (int k = 0; k < seenmers.length; k++)
        {
            if (seenmers[k] == "\0000")
            {
                seenmers[k] = tmp;  // commented to debug rootbeer-string problem
                RootbeerGpu.setSharedObject(oIdx,seenmers);
            }


        }

    }

    public static char[] substring(char[] str, int start, int end)
    {
        char[] ch;
        if (end - start > 0)
        {
            ch = new char[end - start];


            for (int i = 0; i < end - start; i++)
            {
                ch[i] = str[i + start];
            }
            return ch;
        }
        return null;

    }


    public static char[] append_str(char[] ch, String str)
    {
        char[] str_chr = toCharArray(str);
        char[] sb;

        if (ch == null)
        {
            sb = new char[str_chr.length];
            for (int z = 0; z < str_chr.length; z++)
            {
                sb[z] = str_chr[z];
            }

        } else
        {
            sb = new char[ch.length + str_chr.length];
            int q;
            for (q = 0; q < ch.length; q++)
            {
                sb[q] = ch[q];
            }

            for (int z = 0; z < str_chr.length; z++)
            {
                sb[q] = str_chr[z];
                q++;
            }
        }
        return sb;
    }


    @Override
    public void gpuMethod()
    {

        output_key1 = null;
        output_value1 = null;
        output_key2 = null;
        output_value2 = null;

        int end = seq.length - K;



        char[] u = substring(seq, i, i + K);
        char[] v = substring(seq, i + 1, i + 1 + K);
        char[] f = substring(seq, i, i + 1);

        char[] l = substring(seq, i + K, i + K + 1);
        f = rc(f);

        char ud = canonicaldir(u);
        char vd = canonicaldir(v);

        char[] udvd = {ud, vd};

        //   String t  = new String(udvd);
        char[] tr = flip_link(udvd);
//

        char[] uc0 = canonicalseq(u);
        char[] vc0 = canonicalseq(v);

        char[] uc = str2dna(uc0);
        char[] vc = str2dna(vc0);


        if ((i == 0) && (ud == 'r'))
        {
            ustate = '6';
//            RootbeerGpu.setSharedChar(uIdx, ustate);
        }
        if (i + 1 == end)
        {
            vstate = '3';
//            RootbeerGpu.setSharedChar(vIdx, vstate);
        }

        boolean seen = (contains(seenmers, u) || contains(seenmers, v) || Arrays.equals(u, v));
        add(u);

        if (seen)
        {
            chunk++;
            char[] chunk_str = {'c'};//{'c', Character.forDigit(chunk, 10)};
            chunkstr = append(chunk_str, ("" + chunk).toCharArray());
            //#print STDERR "repeat internal to $tag: $uc u$i $chunk\n";
        }


        char[] tab = {'\t'};

        /*
         * output_key1 = uc
         *
         * output_value1 = t + "\t" + l + "\t" + tag + chunkstr + "\t" + ustate
         *
         *
         * t = canonicaldir(u) + canonicaldir(v)
         *
         * l = seq.substring( (i + k), (i + k + 1) )
         *
         * tag = fields[0] ---- nodetxt.split( "\t" )
         *
         * chunkstr = "c" + integer chunk
         *
         * ustate = '5' or '6' or 'm'
         *
        */

        output_key1 = append(output_key1, uc);
        char[] val = new char[5];
        val = append(val, udvd);
        val = append(val, tab);
        val = append(val, l);
        val = append(val, tab);
        val = append(val, tag.toCharArray());
        if (chunkstr != null)
        {
            append(val, chunkstr);
        }

        val = append(val, tab);
        if (ustate != '\000')
        {
//            ustate = RootbeerGpu.getSharedChar(uIdx);
            char[] u_state = {ustate};
            val = append(val, u_state);
        }
        if (val[0] != '\000')
        {
            output_value1 = append(output_value1, val);
        }


        if (seen)
        {
            chunk++;
            chunkstr = append(new char[]{'c'}, ("" + chunk).toCharArray());
        }


        /*
         *  output.collect(new Text(vc),
         *                   new Text(tr + "\t" + f + "\t" + tag + chunkstr + "\t" + vstate));
         *
         * output_key2 = vc
         *
         * tr = flip_link (t) == flip_link (ud + vd )
         *
         *
         */

        char[] val2 = new char[5];
        val2 = append(val2, tr);
        val2 = append(val2, tab);
        val2 = append(val2, f);
        val2 = append(val2, tab);
        //////////////    for (int k = 0; k < tag.length(); k++)
        val2 = append(val2, tag.toCharArray());
        if (chunkstr != null)
        {
            val2 = append(val2, chunkstr);
        }
        val2 = append(val2, tab);
        if (vstate != '\000')
        {
//               char[] v_state = {vstate};
            val2 = append(val2, new char[]{vstate});
        }
        if (vc[0] != '\000')
        {
            output_key2 = append(output_key2, vc);
        }  // void System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
        if (val2[0] != '\000')
        {
            output_value2 = append(output_value2, val2);
        }

        ustate = 'm';
//        RootbeerGpu.setSharedChar(uIdx, ustate);
    }

    public static int findIndex(char[] key)
    {
        int i;
        boolean match = true;

//        System.out.println("Key = " + key);
        char[] table;
        for (i = 0; i < str2dnaKeys.length; i++)
        {


            //        System.out.println("Trying: " + str2dnaKeys[i]);
            table = str2dnaKeys[i].toCharArray();
            for (int q = 0; q < table.length; q++)
            {

                if (table.length != key.length)
                {
                    break;
                } else if (table[q] != key[q])
                {
                    match = false;
                    break;
                } else
                {
                    match = true;
                    //			System.out.println(table[q]);
                    if (q == table.length - 1 && match)
                    {
                        return i;
                    }
                }
            }

        }
        return -1;
    }

    public static char[] str2dna(char[] seq)
    {
        char[] sb = null;
        int l = seq.length;

        int offset = 0;
        int index = 0;


        while (offset < l)
        {
            int r = l - offset;

            if (r >= 4)
            {

                index = findIndex(substring(seq, offset, offset + 4));

                sb = append_str(sb, str2dnaValues[index]);
                offset += 4;
            } else
            {
                index = findIndex(substring(seq, offset, offset + r));
                sb = append_str(sb, str2dnaValues[index]);
                offset += r;
            }
        }

//        return sb.toString();
        return sb;
    }

    public static char[] append(char[] a, char[] b)
    {
        char[] result;
        if (a == null || a[0] == '\000')
        {
            result = new char[b.length];
            System.arraycopy(b, 0, result, 0, b.length);
        } else
        {
            int length = a.length + b.length;
            result = new char[length];
            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);
        }
        return result;
    }

    public static int find_empty(char[] a)
    {
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] == '\000')
            {
                return i;
            }
        }
        return -1; // shouldn't happen
    }


    public static void main(String[] args)
    {
        new BuildGraphMapKernel(null, 0, 0, null, null, null, null, null, null, '\000','\000', 0, null); //String sequence, int kval, int index, String[] seen_mers, String t

    }


}
