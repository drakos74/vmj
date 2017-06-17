package fine.vmj.ml.deep;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fine.vmj.helper.FileIterator;
import fine.vmj.store.data.ByteFrame;

public class FrameEncoder {

	private static final Logger log = LoggerFactory.getLogger(FrameEncoder.class);

	private int seed = ThreadLocalRandom.current().nextInt(100, 1000);
	final private int iterations;

	final private int width;
	final private int height;
	final private int channels;

	private final NativeImageLoader imageLoader;

	private final MultiLayerNetwork model;
	
	private DataSet dataset = null;

	private INDArray indArray = null;
	
	public static void main(String...args){
		FrameEncoder frameEncoder = new FrameEncoder(8,Integer.valueOf(args[0]),Integer.valueOf(args[0]),3);
		
			new FileIterator("/Users/drakos/Projects/dr-eek/free-servers/selfie/VMJ/vmj-fx/store/demo/201764", "vmj.jpg").apply( (File file , int i) -> {
				try {
//					DataSet this_dataSet = frameEncoder.toDataSet(file);
//					if(frameEncoder.dataset == null){
//						frameEncoder.dataset = new DataSet(this_dataSet.getFeatures(),this_dataSet.getLabels());
//					} else{
//						log.info("numExamples:"+frameEncoder.dataset.numExamples()+" vs. "+i);
//						frameEncoder.dataset.addRow(frameEncoder.toDataSet(file), i);
//					}
					
					if(frameEncoder.indArray == null){
						log.info("create new ... "+i);
						frameEncoder.indArray = frameEncoder.toINDArray(file);
					} else{
						log.info("add row ... "+i);
						frameEncoder.indArray.putRow(i, frameEncoder.toINDArray(file));
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
			log.info("indArray:"+frameEncoder.indArray);
			
			frameEncoder.dataset = new DataSet(frameEncoder.indArray,frameEncoder.indArray);
			
			log.info("dataSet:"+frameEncoder.dataset);
			
			log.info("train ... ");
			frameEncoder.train();
		
		log.info("finished training ... check the model...");
		

//		new FileIterator("/Users/drakos/Projects/dr-eek/free-servers/selfie/VMJ/vmj-fx/store/demo/201764", "vmj.jpg").apply( (File file,int i) -> {
//			try {
////				INDArray data = frameEncoder.decode(file);
////				
////				log.info("o_data:"+data.columns()+","+data.rows()+":"+data);
//				
//				int[] prediction = frameEncoder.decode(file);
//				
//				log.info("o_data:"+prediction.length+":"+Arrays.toString(prediction));
//				
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
		

	}

	public FrameEncoder(int iterations, int width, int height, int channels) {

		log.info("initialise frameEncoder ... [" + iterations + "," + width + "," + height + "," + channels + "]");

		this.iterations = iterations;
		this.width = width;
		this.height = height;
		this.channels = channels;

		imageLoader = new NativeImageLoader(height, width, channels);
		log.info("init ImageLoader ... ");

		MultiLayerConfiguration config = getConf(LossFunctionType.RMSE_MINI);
		log.info("got Configuration ... ");
		model = new MultiLayerNetwork(config);
		log.info("create Network ... ");
		model.init();
		log.info("model init ... ");
		model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(iterations / 5)));
		log.info("model setListener ... ");
	}

	private MultiLayerConfiguration getConf(LossFunctionType type) {
		MultiLayerConfiguration conf = null;
		log.info("ALGO_TYPE:" + type + " on " + (width * height));
		switch (type) {
		case RMSE_FULL:
			conf = new NeuralNetConfiguration.Builder().seed(seed).iterations(iterations)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).list(10)
					.layer(0,
							new RBM.Builder().nIn(height * width).nOut(1000)
									.lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
					.layer(1,
							new RBM.Builder().nIn(1000).nOut(500).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(2,
							new RBM.Builder().nIn(500).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(3,
							new RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(4,
							new RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())

					// encoding stops
					.layer(5,
							new RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())

					// decoding starts
					.layer(6,
							new RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(7,
							new RBM.Builder().nIn(250).nOut(500).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(8,
							new RBM.Builder().nIn(500).nOut(1000).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT).nIn(1000)
							.nOut(height * width).build())
					.pretrain(true).backprop(true).build();
			break;
		case RMSE_MINI:
			conf = new NeuralNetConfiguration.Builder().seed(seed).iterations(iterations)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).list(10)
					.layer(0,
							new RBM.Builder().nIn(height * width * 3).nOut(400)
									.lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
					.layer(1,
							new RBM.Builder().nIn(400).nOut(200).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(2,
							new RBM.Builder().nIn(200).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(3,
							new RBM.Builder().nIn(100).nOut(50).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(4,
							new RBM.Builder().nIn(50).nOut(100).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(5,
							new RBM.Builder().nIn(100).nOut(200).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(6,
							new RBM.Builder().nIn(200).nOut(400).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
									.build())
					.layer(7, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT).nIn(400)
							.nOut(height * width * 3).build())
					.pretrain(true).backprop(true).build();
			break;
		case DIVERG:
			// conf = new
			// NeuralNetConfiguration.Builder().seed(seed).iterations(iterations)
			// .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT).list()
			// .layer(0,
			// new RBM.Builder().nIn(height * width).nOut(1000)
			// .lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build())
			// .layer(1,
			// new
			// RBM.Builder().nIn(1000).nOut(500).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(2,
			// new
			// RBM.Builder().nIn(500).nOut(250).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(3,
			// new
			// RBM.Builder().nIn(250).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(4,
			// new
			// RBM.Builder().nIn(100).nOut(30).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// // encoding stops
			// .layer(5,
			// new
			// RBM.Builder().nIn(30).nOut(100).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// // decoding starts
			// .layer(6,
			// new
			// RBM.Builder().nIn(100).nOut(250).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(7,
			// new
			// RBM.Builder().nIn(250).nOut(500).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(8,
			// new
			// RBM.Builder().nIn(500).nOut(1000).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE)
			// .build())
			// .layer(9,
			// new
			// OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.SIGMOID)
			// .nIn(1000).nOut(numRows * numColumns).build())
			// .pretrain(true).backprop(true).build();
			break;
		}

		log.info("Configuration for " + type + " is :\n" + conf);
		return conf;
	}
	
	public void train(){
		model.fit(dataset);
	}

	public int[] decode(File file) throws IOException {

		log.info("decoding img [" + file.exists() + "] from " + file);

		INDArray data = new NativeImageLoader(height, width, channels).asRowVector(file);
//imageLoader.asRowVector(file);

		log.info("i_data:" + data);

		log.info("data.isMatrix():" + data.isMatrix() + ",data.isRowVector():" + data.isRowVector()
				+ ",data.isVector():" + data.isVector());

		
		return model.predict(data);
	}
	
	public INDArray toINDArray(File file) throws IOException{
		log.info("toINDArray img [" + file.exists() + "] from " + file);

		INDArray data = new NativeImageLoader(height, width, channels).asRowVector(file);

		log.info("data:" + data);

		log.info("data.isMatrix():" + data.isMatrix() + ",data.isRowVector():" + data.isRowVector()
				+ ",data.isVector():" + data.isVector());
		
		return data;
	}
	
	public DataSet toDataSet(File file) throws IOException{
		log.info("toDataSet img [" + file.exists() + "] from " + file);
		INDArray ndArray = toINDArray(file);
		DataSet ds = new DataSet(ndArray,ndArray);
		log.info("dataSet.labels:"+ds.getLabels());
		log.info("dataSet.features:"+ds.getFeatures());
		return ds;
	}

	public void encode(File file) throws IOException {

		log.info("encoding img [" + file.exists() + "] from " + file);

		INDArray data = imageLoader.asRowVector(file);

		log.info("data:" + data);

		log.info("data.isMatrix():" + data.isMatrix() + ",data.isRowVector():" + data.isRowVector()
				+ ",data.isVector():" + data.isVector());

		DataSet dataSet = new DataSet(data, data);

		
		
		log.info("dataSet:" + dataSet);
		
		log.info("features:" + dataSet.getFeatureMatrix());
		log.info("labels:" + dataSet.getLabels());
		log.info("masks:" + dataSet.getLabelsMaskArray());

//		DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
//		scaler.transform(dataSet);
//
//		log.info("scaled:" + dataSet);

		model.fit(data);

		log.info("Labels:" + model.getLabels());
	}

	public void encode(ByteFrame frame) throws IOException {

		log.info("encoding frame " + frame);

		byte[] frame_bytes = frame.getBytes();

		log.info("got frame_bytes ... " + frame_bytes.length);

		INDArray data = imageLoader.asMatrix(new ByteArrayInputStream(frame_bytes));

		log.info("data:" + data);

		model.fit(new DataSet(data, data));

		log.info("Labels:" + model.getLabels());
	}

	public void encode(Mat frame) throws IOException {

		log.info("encoding mat " + frame);

		INDArray data = new NativeImageLoader(width, height, 3).asMatrix(frame);

		log.info("data:" + data);

		model.fit(new DataSet(data, data));

		log.info("Labels:" + model.getLabels());
	}

	public enum LossFunctionType {
		RMSE_FULL, RMSE_MINI, DIVERG
	}

}
