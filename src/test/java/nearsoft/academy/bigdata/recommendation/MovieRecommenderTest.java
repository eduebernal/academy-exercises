package nearsoft.academy.bigdata.recommendation;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class MovieRecommenderTest {

    public class MovieRecommender{

        long userCount = 0;
        long productCount = 0;
        List<String> productIds = new ArrayList<String>();
        List<String> userIds = new ArrayList<String>();
        List<String> scores = new ArrayList<String>();

        HashMap<String,Long> uniqueProducts = new HashMap<String,Long>();
        HashMap<Long,String> uniqueProductsInv = new HashMap<Long,String>();
        HashMap<String,Long> uniqueUsers = new HashMap<String,Long>();

        public MovieRecommender(String source){
            try {
                FileInputStream fis = new FileInputStream(source);
                GZIPInputStream gzis = new GZIPInputStream(fis);
                InputStreamReader reader = new InputStreamReader(gzis);
                BufferedReader in = new BufferedReader(reader);

                String readed;
                while ((readed = in.readLine()) != null) {
                    if(readed.contains("product/productId")){
                        String id = readed.substring(readed.indexOf(":")+1,readed.length()).trim();
                        if(!uniqueProducts.containsKey(id)){
                            productCount++;
                            uniqueProducts.put(id,productCount);
                            uniqueProductsInv.put(productCount,id);
                        }
                        productIds.add(Long.toString(uniqueProducts.get(id)));
                    }
                    if(readed.contains("review/userId")){
                        String user = readed.substring(readed.indexOf(":")+1,readed.length()).trim();
                        if(!uniqueUsers.containsKey(user)){
                            userCount++;
                            uniqueUsers.put(user,userCount);
                        }
                        userIds.add(Long.toString(uniqueUsers.get(user)));
                    }
                    if(readed.contains("review/score")){
                        String score = readed.substring(readed.indexOf(":")+1,readed.length()).trim();
                        scores.add(score);
                    }
                }
                in.close();
            } catch (Exception e) {
                e.getStackTrace();
            }
        }

        public void writeCSV(){
            try {
                PrintWriter writer = new PrintWriter("reviews.csv");
                int i = 0;
                while(i<productIds.size()){
                    writer.append(userIds.get(i));
                    writer.append(",");
                    writer.append(productIds.get(i));
                    writer.append(",");
                    writer.append(scores.get(i));
                    writer.append("\n");
                    i++;
                }
                writer.close();
            } catch (Exception e) {
                e.getStackTrace();
            }
        }

        public int getTotalReviews(){
            return productIds.size();
        }

        public int getTotalProducts(){
            return uniqueProducts.size();
        }

        public int getTotalUsers(){
            return uniqueUsers.size();
        }

        public List<String> getRecommendationsForUser(String user){
            List<String> recs = new ArrayList<String>();
            writeCSV();
            try {
                DataModel model = new FileDataModel(new File("reviews.csv"));
                UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
                UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
                Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
                Recommender cachingRecommender = new CachingRecommender(recommender);
                List<RecommendedItem> recommendations = cachingRecommender.recommend(uniqueUsers.get(user), 3);
                for (RecommendedItem recommendation : recommendations) {
                    recs.add(uniqueProductsInv.get(recommendation.getItemID()));
                  }
            } catch (Exception e) {
                System.out.println(e);
            }
            return recs;
        }
    }
    @Test
    public void testDataInfo() throws IOException, TasteException {
        //download movies.txt.gz from 
        //    http://snap.stanford.edu/data/web-Movies.html
        MovieRecommender recommender = new MovieRecommender("movies.txt.gz");
        assertEquals(7911684, recommender.getTotalReviews());
        assertEquals(253059, recommender.getTotalProducts());
        assertEquals(889176, recommender.getTotalUsers());

        List<String> recommendations = recommender.getRecommendationsForUser("A141HP4LYPWMSR");
        assertThat(recommendations, hasItem("B0002O7Y8U"));
        assertThat(recommendations, hasItem("B00004CQTF"));
        assertThat(recommendations, hasItem("B000063W82"));

    }

}
