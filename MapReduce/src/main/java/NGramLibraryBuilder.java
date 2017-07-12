import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class NGramLibraryBuilder {
    public static class NGramMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        int noGram;
        @Override
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            noGram = conf.getInt("noGram", 5);
        }

        // map method
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            Configuration conf = context.getConfiguration();

            int noGram = conf.getInt("noGram", 5);

            String line = value.toString().trim().toLowerCase().replaceAll("[^a-z]", " ");

            String[] words = line.split("\\s+");
            if (words.length < 2) {
                return;
            }

            StringBuilder stringBuilder;
            for (int i = 0; i < words.length; i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(words[i]);
                for (int j = 1; i + j < words.length && j < noGram; j++) {
                    stringBuilder.append(" ");
                    stringBuilder.append(words[i + j]);
                    context.write(new Text(stringBuilder.toString()), new IntWritable(1));
                }
            }


        }
    }

    public static class NGramReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        // reduce method
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int sum = 0;
            for(IntWritable value: values) {
                sum += value.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

}