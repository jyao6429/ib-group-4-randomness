import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main
{
	static String fileName;
	static AWTFrameGrab grab;
	static ArrayList<Double> entropies;

	public static void main(String[] args)
	{
		Scanner sc = new Scanner(System.in);
		System.out.println("Starting IB Group 4 - Randomness");

		System.out.println("Please input the video file name without the file extension: ");
		fileName = "files/" + sc.nextLine();

		System.out.println("Do you want to create a difference video? (true or false): ");
		boolean differenceV = sc.nextBoolean();

		// Grab the frames
		grabAllFrames();

		// Initialize the ArrayList holding all the entropies
		entropies = new ArrayList<>();

		// Try calculating all the entropies from the video file, while writing the difference images to a video file
		System.out.println("Begin entropy calculation and difference image encoding");
		SeekableByteChannel out = null;
		AWTSequenceEncoder encoder = null;
		int frameCounter = 1;

		try
		{
			// Previous and current images
			BufferedImage prevImg = grab.getFrame();
			BufferedImage currentImg;

			// Creating video from difference images
			if (differenceV)
			{
				out = NIOUtils.writableFileChannel(fileName + " - DIFF.mp4");
				encoder = new AWTSequenceEncoder(out, Rational.R(30, 1));
			}
			while ((currentImg = grab.getFrame()) != null)
			{
				if (frameCounter % 15 == 0)
					System.out.print("~");
				// Calculate entropy of difference image
				BufferedImage diffImg = getDifferenceImage(prevImg, currentImg);
				entropies.add(getEntropy(diffImg, 256));

				// Encode difference image into video file
				if (differenceV)
					encoder.encodeImage(diffImg);

				// Update prevImg
				prevImg = currentImg;
				frameCounter++;
			}
			if (differenceV)
				encoder.finish();
		}
		catch (Exception ex)
		{
			System.out.println("Error while getting frames");
			ex.printStackTrace();
		}
		finally
		{
			NIOUtils.closeQuietly(out);
			System.out.println();
		}
		System.out.println("Finished entropy calculation and difference image encoding");

		// Try writing entropies to a file and calculate statistics
		DescriptiveStatistics stats = new DescriptiveStatistics();

		System.out.println("Begin writing entropies to file");
		try
		{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName + " - ENTROPY.txt")));
			for (double tempD : entropies)
			{
				stats.addValue(tempD);
				pw.println(tempD);
			}
			pw.close();
		}
		catch (IOException ex)
		{
			System.out.println("Unable to write to file");
		}
		System.out.println("Finished writing entropies to file");

		System.out.printf("avg: %6.6f\tstd: %6.6f\tmedian: %6.6f", stats.getMean(), stats.getStandardDeviation(), stats.getPercentile(50));
	}
	private static void grabAllFrames()
	{
		try
		{
			grab = AWTFrameGrab.createAWTFrameGrab(NIOUtils.readableChannel(new File(fileName + ".mp4")));
			System.out.println("Grabbed all frames from \"" + fileName + ".mp4\"");
		}
		catch (FileNotFoundException ex)
		{
			System.out.println("Unable to find file");
			ex.printStackTrace();
		}
		catch (JCodecException | IOException ex)
		{
			System.out.println("Unable to grab frames");
			ex.printStackTrace();
		}
	}
	// The following code was copied from https://stackoverflow.com/questions/25022578/highlight-differences-between-images
	public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2)
	{
		int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
		int width2 = img2.getWidth(); // take no arguments
		int height1 = img1.getHeight();
		int height2 = img2.getHeight();
		if ((width1 != width2) || (height1 != height2))
		{
			System.err.println("Error: Images dimensions mismatch");
			System.exit(1);
		}

		// NEW - Create output Buffered image of type RGB
		BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

		// Modified - Changed to int as pixels are ints
		int diff;
		int result; // Stores output pixel
		for (int i = 0; i < height1; i++)
		{
			for (int j = 0; j < width1; j++)
			{
				int rgb1 = img1.getRGB(j, i);
				int rgb2 = img2.getRGB(j, i);
				int r1 = (rgb1 >> 16) & 0xff;
				int g1 = (rgb1 >> 8) & 0xff;
				int b1 = (rgb1) & 0xff;
				int r2 = (rgb2 >> 16) & 0xff;
				int g2 = (rgb2 >> 8) & 0xff;
				int b2 = (rgb2) & 0xff;
				diff = Math.abs(r1 - r2); // Change
				diff += Math.abs(g1 - g2);
				diff += Math.abs(b1 - b2);
				diff /= 3; // Change - Ensure result is between 0 - 255
				// Make the difference image gray scale
				// The RGB components are all the same
				result = (diff << 16) | (diff << 8) | diff;
				outImg.setRGB(j, i, result); // Set result
			}
		}

		// Now return
		return outImg;
	}
	// The following code was copied from https://github.com/Jeanvit/CakeImageAnalyzer/blob/master/src/cake/classes/ImageUtils.java
	/* Process a image to obtain a 8 bit grayscale representation
	 * @param BufferedImage originalImage - The image to be processed
	 * @return BufferedImage img - The 8bit grayscale of the given image
	 */
	public static BufferedImage getGrayScale8bits(BufferedImage inputImage)
	{
		BufferedImage img = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = img.getGraphics();
		g.drawImage(inputImage, 0, 0, null);
		g.dispose();
		return img;
	}
	/* Create a array of ints, representing the histogram of a given image
	 * @param BufferedImage originalImage - The image to be processed
	 * @param int numberOfBins - Number of histogram bins
	 * @return int bins[] - The array containing the occurrence of each intensity pixel (the histogram)
	 */
	public static int[] buildHistogram(BufferedImage image, int numberOfBins)
	{
		int bins[] = new int[numberOfBins];
		int intensity;
		image = getGrayScale8bits(image);
		for (int i = 0; i <= image.getWidth() - 1; i++)
		{
			for (int j = 0; j <= image.getHeight() - 1; j++)
			{
				intensity = image.getRGB(i, j) & 0xFF;
				bins[intensity]++;
			}
		}
		return bins;
	}
	/* Compute the entropy of an image based on the Shannon's Formula
	 * @param BufferedImage originalImage - The image to be processed
	 * @param int maxValue - The maximum value of intensity pixels, the same number as the histogram bins
	 * @return int entropyValue - The entropy value
	 */
	public static double getEntropy(BufferedImage image, int maxValue)
	{
		int bins[] = buildHistogram(image, maxValue);
		double entropyValue = 0, temp = 0;
		double totalSize = image.getHeight() * image.getWidth(); //total size of all symbols in an image

		for (int i = 0; i < maxValue; i++)
		{ //the number of times a symbol has occurred
			if (bins[i] > 0)
			{ //log of zero goes to infinity
				temp = (bins[i] / totalSize) * (Math.log(totalSize / bins[i]));
				entropyValue += temp;
			}
		}
		return entropyValue;
	}
}
