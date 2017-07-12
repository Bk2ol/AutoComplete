import org.apache.commons.lang.ObjectUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public class LanguageModel {
    public static class Map extends Mapper<LongWritable, Text, Text, Text> {

        int threshold;

        @Override
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            threshold = conf.getInt("threshold", 20);
        }


        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            if((value == null) || (value.toString().trim()).length() == 0) {
                return;
            }

            String line = value.toString().trim();

            String[] wordsAndCount = line.split("\t");
            if(wordsAndCount.length < 2) {
                return;
            }

            String[] words = wordsAndCount[0].split("\\s+");
            int count = Integer.valueOf(wordsAndCount[1]);

            if (count < threshold) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length - 1; i++) {
                sb.append(words[i]);
                sb.append(" ");
            }

            String outputKey = sb.toString().trim();
            String outputValue = words[words.length - 1] + "=" + count;

            context.write(new Text(outputKey), new Text(outputValue));
        }
    }

    public static class Reduce extends Reducer<Text, Text, DBOutputWritable, NullWritable> {

        int topK;

        @Override
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            topK = conf.getInt("topK", 5);
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            TreeMap<Integer, List<String>> map = new TreeMap<Integer, List<String>>(Collections.<Integer>reverseOrder());

            for (Text val : values) {
                String value = val.toString().trim();
                String word =  value.split("=")[0].trim();
                int count = Integer.parseInt(value.split("=")[1].trim());
                if (map.containsKey((count))) {
                    map.get(count).add(word);
                } else {
                    List<String> list = new ArrayList<String>();
                    list.add(word);
                    map.put(count, list);
                }
            }

            Iterator<Integer> iter = map.keySet().iterator();
            for (int j = 0; iter.hasNext() && j < topK; ) {
                int keyCount = iter.next();
                List<String> words = map.get(keyCount);
                for (int i = 0; i < words.size() && j < topK; i++) {
                    context.write(new DBOutputWritable(key.toString(), words.get(i), keyCount), NullWritable.get());
                    j++;
                }
            }
        }
    }
}
