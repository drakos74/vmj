package fine.vmj.ml.demo.deep;

import java.io.IOException;
import java.util.Arrays;

import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepAutoEncoderDemo {

	private static final Logger log = LoggerFactory.getLogger(DeepAutoEncoderDemo.class);

	public static void main(String... args)  {
		try{
			train();
		}catch(Exception e){
			log.error(e.getMessage(),e);
		}
	}
	
	private static void train() throws IOException{
		final int numRows = 28;
		final int numColumns = 28;
		int seed = 123;
		int numSamples = MnistDataFetcher.NUM_EXAMPLES;
		int batchSize = 1000;
		int iterations = 1;
		int listenerFreq = iterations / 5;

		log.info("Load data....");
		DataSetIterator iter = new MnistDataSetIterator(batchSize, numSamples, true);

		log.info("Build model....");
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(seed).iterations(iterations)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).list(10)
				.layer(0,
						new RBM.Builder().nIn(numRows * numColumns).nOut(1000)
								.lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
				.layer(1,
						new RBM.Builder().nIn(1000).nOut(500).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
								.build())
				.layer(2,
						new RBM.Builder().nIn(500).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
				.layer(3,
						new RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
				.layer(4,
						new RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())

				// encoding stops
				.layer(5,
						new RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())

				// decoding starts
				.layer(6,
						new RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
				.layer(7,
						new RBM.Builder().nIn(250).nOut(500).lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
				.layer(8,
						new RBM.Builder().nIn(500).nOut(1000).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
								.build())
				.layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT).nIn(1000)
						.nOut(numRows * numColumns).build())
				.pretrain(true).backprop(true).build();

		MultiLayerNetwork model = new MultiLayerNetwork(conf);
		model.init();

		model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));

		log.info("Train model....");
//		while (iter.hasNext()) {
			DataSet next = iter.next();
			log.info(next.getFeatureMatrix().toString());
			model.fit(new DataSet(next.getFeatureMatrix(), next.getFeatureMatrix()));
//		}
		
		for(int i  = 0 ; i < 10 ; i++){
			log.info("Layer "+i+" WEIGHTS : "+model.getLayer(i).getParam(DefaultParamInitializer.WEIGHT_KEY));
			Layer layer = model.getLayer(i);
			
		}
		
		
	}
}
