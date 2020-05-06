package sample;

import javafx.beans.Observable;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.stage.FileChooser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

public class Controller {
    @FXML private ImageView originalImageView;
    @FXML private ImageView transformedImageView;
    @FXML private Slider brightnessSlider;
    @FXML private Slider contrastSlider;

    private Image originalImage;
    private Image transformedImage;
    private ColorAdjust colorAdjust;
    private String filePath;

    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private LineChart<String, Number> chartHistogram;

    @FXML
    private CategoryAxis xAxisBrightness;
    @FXML
    private NumberAxis yAxisBrightness;
    @FXML
    private LineChart<String, Number> brightnessHistogram;

    public void initialize() {
        colorAdjust = new ColorAdjust();
        brightnessSlider.valueProperty().addListener((number) -> onBrightnessSet(number));
        contrastSlider.valueProperty().addListener((number) -> onContrastSet(number));
    }

    @FXML
    public void showFileDialog() throws IOException {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Pliki JPG oraz PNG", "*.jpg", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            originalImage = new Image(file.toURI().toString());
            transformedImage = new Image(file.toURI().toString());
            originalImageView.setImage(originalImage);
            transformedImageView.setImage(transformedImage);
            chartHistogram.setCreateSymbols(false);
            brightnessHistogram.setCreateSymbols(false);

            recalculateHistogram();
        }
    }

    @FXML
    public void equalizeHistogram() {
        Image image = transformedImageView.snapshot(null, null);
        ImageHistogram imageHistogram = new ImageHistogram(image);

        image = SwingFXUtils.toFXImage(imageHistogram.equalize(), null);

        transformedImageView.setImage(image);
        recalculateHistogram();
    }

    public void recalculateHistogram() {
//        chartHistogram.getData().clear();
//        brightnessHistogram.getData().clear();

        Image image = transformedImageView.snapshot(null, null);
        ImageHistogram imageHistogram = new ImageHistogram(image);
        if(imageHistogram.isSuccess()){
            chartHistogram.getData().setAll(
                    imageHistogram.getSeriesRed(),
                    imageHistogram.getSeriesGreen(),
                    imageHistogram.getSeriesBlue());

            brightnessHistogram.getData().setAll(imageHistogram.getSeriesBrightness());
        }
    }
    @FXML
    public void onBrightnessSet(Observable value) {
        colorAdjust.setBrightness(brightnessSlider.getValue());
        transformedImageView.setEffect(colorAdjust);

        recalculateHistogram();
    }

    @FXML
    public void onContrastSet(Observable value) {
        colorAdjust.setContrast(contrastSlider.getValue());
        transformedImageView.setEffect(colorAdjust);

        recalculateHistogram();
    }
}

class ImageHistogram {

    private Image image;

    private long alpha[] = new long[256];
    private long red[] = new long[256];
    private long green[] = new long[256];
    private long blue[] = new long[256];

    private long brightness[] = new long[256];

    XYChart.Series seriesAlpha;
    XYChart.Series seriesRed;
    XYChart.Series seriesGreen;
    XYChart.Series seriesBlue;

    XYChart.Series seriesBrightness;

    private boolean success;

    ImageHistogram(Image src) {
        image = src;
        success = false;

        //init
        for (int i = 0; i < 256; i++) {
            alpha[i] = red[i] = green[i] = blue[i] = 0;
            brightness[i] = 0;
        }

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            return;
        }

        //count pixels
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = pixelReader.getArgb(x, y);
                int a = (0xff & (argb >> 24));
                int r = (0xff & (argb >> 16));
                int g = (0xff & (argb >> 8));
                int b = (0xff & argb);

                alpha[a]++;
                red[r]++;
                green[g]++;
                blue[b]++;

                //Convert RGB to HSB (or HSV)
                float[] hsb = new float[3];
                Color.RGBtoHSB(r, g, b, hsb);
                brightness[(int)(hsb[2]*255)]++;
            }
        }

        seriesAlpha = new XYChart.Series();
        seriesRed = new XYChart.Series();
        seriesGreen = new XYChart.Series();
        seriesBlue = new XYChart.Series();
        seriesBrightness = new XYChart.Series();
        seriesAlpha.setName("alpha");
        seriesRed.setName("red");
        seriesGreen.setName("green");
        seriesBlue.setName("blue");
        seriesBrightness.setName("Brightness");

        //copy alpha[], red[], green[], blue[], brightness
        //to seriesAlpha, seriesRed, seriesGreen, seriesBlue, seriesBrightness
        for (int i = 0; i < 256; i++) {
            seriesAlpha.getData().add(new XYChart.Data(String.valueOf(i), alpha[i]));
            seriesRed.getData().add(new XYChart.Data(String.valueOf(i), red[i]));
            seriesGreen.getData().add(new XYChart.Data(String.valueOf(i), green[i]));
            seriesBlue.getData().add(new XYChart.Data(String.valueOf(i), blue[i]));

            seriesBrightness.getData().add(new XYChart.Data(String.valueOf(i), brightness[i]));
        }

        success = true;
    }

    BufferedImage getGrayscaleImage(BufferedImage src) {
        BufferedImage gImg = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = src.getRaster();
        WritableRaster gr = gImg.getRaster();
        for(int i=0;i<wr.getWidth();i++){
            for(int j=0;j<wr.getHeight();j++){
                gr.setSample(i, j, 0, wr.getSample(i, j, 0));
            }
        }
        gImg.setData(gr);
        return gImg;
    }

    BufferedImage equalize() {
        BufferedImage nImg = getGrayscaleImage(SwingFXUtils.fromFXImage(image, null));

        WritableRaster er = nImg.getRaster();
        int totpix= er.getWidth()*er.getHeight();
        int[] histogram = new int[256];

        for (int x = 0; x < er.getWidth(); x++) {
            for (int y = 0; y < er.getHeight(); y++) {
                histogram[er.getSample(x, y, 0)]++;
            }
        }

        int[] chistogram = new int[256];
        chistogram[0] = histogram[0];
        for(int i=1;i<256;i++){
            chistogram[i] = chistogram[i-1] + histogram[i];
        }

        float[] arr = new float[256];
        for(int i=0;i<256;i++){
            arr[i] =  (float)((chistogram[i]*255.0)/(float)totpix);
        }

        for (int x = 0; x < er.getWidth(); x++) {
            for (int y = 0; y < er.getHeight(); y++) {
                int nVal = (int) arr[er.getSample(x, y, 0)];
                er.setSample(x, y, 0, nVal);
            }
        }
        nImg.setData(er);
        return nImg;
    }

    public boolean isSuccess() {
        return success;
    }

    public XYChart.Series getSeriesAlpha() {
        return seriesAlpha;
    }

    public XYChart.Series getSeriesRed() {
        return seriesRed;
    }

    public XYChart.Series getSeriesGreen() {
        return seriesGreen;
    }

    public XYChart.Series getSeriesBlue() {
        return seriesBlue;
    }

    public XYChart.Series getSeriesBrightness() {
        return seriesBrightness;
    }
}
