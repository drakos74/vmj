package fine.vmj.ml.demo.shallow;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.conf.layers.RBM.HiddenUnit;
import org.deeplearning4j.nn.conf.layers.RBM.VisibleUnit;
import org.deeplearning4j.nn.layers.factory.LayerFactories;
//import org.deeplearning4j.nn.layers.factory.LayerFactories;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictedBoltzmanMachine {

	private static Logger log = LoggerFactory.getLogger(RestrictedBoltzmanMachine.class);

	public static void main(String[] args) throws IOException {
		// Customizing params
		Nd4j.MAX_SLICES_TO_PRINT = -1;
		Nd4j.MAX_ELEMENTS_PER_SLICE = -1;
		Nd4j.ENFORCE_NUMERICAL_STABILITY = true;
		final int numRows = 4;
		final int numColumns = 1;
		int outputNum = 10;
		int numSamples = 150;
		int batchSize = 150;
		int iterations = 100;
		int seed = 123;
		int listenerFreq = iterations / 2;

		log.info("Load data....");
		DataSetIterator iter = new IrisDataSetIterator(batchSize, numSamples);
		// Loads data into generator and format consumable for NN
		DataSet iris = iter.next();
		
		int count = 0;
		log.info("Set_Size:"+iris.numInputs());
		for(DataSet set :iris.asList()){
			System.out.println(set.toString());
			log.info(set.getFeatures().toString());
			log.info("-----------------");
			count++;
		}
		log.info("iris_count="+count);
		
//		log.info(Arrays.toString(iris.getColumnNames().toArray(new String[0])));
//		iris.getColumnNames();

		iris.normalizeZeroMeanZeroUnitVariance();

		log.info("Normalised_Set_Size:"+iris.numExamples());
		for(DataSet set :iris.asList()){
//			System.out.println(set.toString());
//			log.info(set.getFeatures().toString());
//			log.info("-----------------");
		}
		
		log.info("Build model....");
		NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().regularization(true).miniBatch(true)
				// Gaussian for visible; Rectified for hidden
				// Set contrastive divergence to 1
				.layer(new RBM.Builder().l2(1e-1).l1(1e-3).nIn(numRows * numColumns) // Input
																						// nodes
						.nOut(outputNum) // Output nodes
						.activation("relu") // Activation function type
						.weightInit(WeightInit.RELU) // Weight initialization
						.lossFunction(LossFunction.RECONSTRUCTION_CROSSENTROPY).k(3)
						.hiddenUnit(HiddenUnit.RECTIFIED).visibleUnit(VisibleUnit.GAUSSIAN).updater(Updater.ADAGRAD)
						.gradientNormalization(GradientNormalization.ClipL2PerLayer).build())
				.seed(seed) // Locks in weight initialization for tuning
				.iterations(iterations).learningRate(1e-3) // Backprop step size
				// Speed of modifying learning rate
				.optimizationAlgo(OptimizationAlgorithm.LBFGS)
				// ^^ Calculates gradients
				.build();
		
		int numParams = LayerFactories.getFactory(conf).initializer().numParams(conf,true);
        INDArray params = Nd4j.create(1, numParams);

		Layer model = LayerFactories.getFactory(conf.getLayer()).create(
				conf,
				(Collection<IterationListener>)null,
				0,
				params,
				true);
		model.setListeners(new ScoreIterationListener(listenerFreq));

		log.info("Evaluate weights....");
		INDArray w = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
		log.info("Weights: " + w);
		log.info("Scaling the dataset");
		iris.scale();
		log.info("Train model....");
		for (int i = 0; i < 20; i++) {
			log.info("Epoch " + i + ":");
			model.fit(iris.getFeatureMatrix());
			log.info("Weights: "+model.getParam(DefaultParamInitializer.WEIGHT_KEY));
		}
		log.info("Weights: "+model.getParam(DefaultParamInitializer.WEIGHT_KEY));
		log.info("Training concluded...");
		
	}
	// A single layer learns features unsupervised.
}
