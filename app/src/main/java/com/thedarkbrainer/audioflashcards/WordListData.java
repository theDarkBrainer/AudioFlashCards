package com.thedarkbrainer.audioflashcards;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class WordListData implements Serializable  {

    private final String FILENAME = "WordListData.txt";

    public static class Data implements Serializable
    {
        public Data() {
            mUses = mErrors = mSkipped = 0;
        }
        public Data(String german, String english, int uses, int errs, int skipped) { mGerman = german; mEnglish = english; mUses = uses; mErrors = errs; mSkipped = skipped; }

        public boolean isEmpty() { return mGerman.length() == 0  && mEnglish.length() == 0; }

        public String getId() { return mId; }
        public String getGerman() { return mGerman; }
        public String getEnglish() { return mEnglish; }
        public void setGerman(String value) { mGerman = value; }
        public void setEnglish(String value) { mEnglish = value; }
        public void increaseUses() { mUses ++; }
        public void increateErrors() { mErrors ++; }
        public int getUses() { return mUses; }
        public int getErrors() { return mErrors; }

        private String mId;
        private String mGerman;
        private String mEnglish;
        private int mUses;
        private int mErrors;
        private int mSkipped;

        public double mDifficulty = 0.3f;
        public double mDaysBetweenReviews = 1.0f;
        public int mDateLastReviewed = 0;
        public double mPercentOverdue = 0;
    }

    public interface ComplexIterator
    {
        Data get();
        Data getRandomExcudingCurrent();
        Data next();
        void setCurrentAnswer(boolean correct);
    }

    private List<Data> mDataList = new ArrayList<>();

    public WordListData(Context context) {

        this.load( context );

        /*if ( mDataList.size() == 0 )
        {
            Data d1 = new Data();
            d1.mGerman = "das Fahrrad";
            d1.mEnglish = "the bicycle";
            mDataList.add(d1);

            Data d2 = new Data();
            d2.mGerman = "das Brod";
            d2.mEnglish = "the bread";
            mDataList.add(d2);

            Data d3 = new Data();
            d3.mGerman = "das Brod";
            d3.mEnglish = "the bread";
            mDataList.add(d3);
        }*/
    }

    private void load(Context context) {
        FileInputStream inputStream;
        BufferedReader reader;
        try {
            inputStream = context.openFileInput(FILENAME);
            this.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            mDataList.clear();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            mDataList.clear();
        }

    }

    public void save(Context context) {
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            this.save(outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load(FileInputStream stream) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String ver = reader.readLine();
        if ( ver != null ) {
            if ( ver.equals("ver0") || ver.equals("ver1") || ver.equals("ver2")) {
                String line = reader.readLine();
                while (line != null) {
                    Data data = new Data();
                    data.mId = String.valueOf(mDataList.size());
                    data.mGerman = line;
                    if ( ver.equals("ver0") || ver.equals("ver1") )
                        reader.readLine();
                    data.mEnglish = reader.readLine();
                    data.mUses = Integer.parseInt(reader.readLine());
                    data.mErrors = Integer.parseInt(reader.readLine());
                    data.mSkipped = Integer.parseInt(reader.readLine());
                    data.mDifficulty = Double.parseDouble(reader.readLine());
                    data.mDaysBetweenReviews = Double.parseDouble(reader.readLine());
                    if ( data.mUses == 0 )
                        data.mPercentOverdue = 1;
                    else
                        data.mPercentOverdue = 1.0 - data.mErrors / data.mUses;
                    mDataList.add(data);

                    line = reader.readLine();
                }
            }
        }

        reader.close();

    }

    public void save(FileOutputStream stream) throws IOException {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        writer.write("ver2");
        writer.newLine();

        for (Data data : mDataList) {
            writer.write(data.mGerman);
            writer.newLine();
            if ( data.mEnglish != null )
                writer.write(data.mEnglish);
            else
                writer.write("");
            writer.newLine();
            writer.write(Integer.toString(data.mUses));
            writer.newLine();
            writer.write(Integer.toString(data.mErrors));
            writer.newLine();
            writer.write(Integer.toString(data.mSkipped));
            writer.newLine();
            writer.write(Double.toString(data.mDifficulty));
            writer.newLine();
            writer.write(Double.toString(data.mDaysBetweenReviews));
            writer.newLine();
        }

        writer.close();
    }

    public int getCount() {
        return mDataList.size();
    }

    public Data getItem(int position) {
        return mDataList.get(position);
    }

    public Data getItem(String id) {
        return mDataList.get(Integer.valueOf(id));
    }

    public void addItem(Data data) {
        data.mId = String.valueOf(mDataList.size());
        mDataList.add(data);
    }

    public void clear() {
        mDataList.clear();
        WordListAudioRenderer.clearAudioFolder();
    }

    public void removeItem(int position) {
        mDataList.remove( position );
        WordListAudioRenderer.clearAudioFolder();
    }

    public class RandomIterator implements WordListData.ComplexIterator
    {
        private Random mRandomGenerator = new Random(System.currentTimeMillis());
        private int mCurrentIndex = 0;

        RandomIterator() {
            mCurrentIndex = mRandomGenerator.nextInt(mDataList.size());
        }

        @Override
        public Data get() {
            return mDataList.get(mCurrentIndex);
        }

        @Override
        public Data getRandomExcudingCurrent() {
            int index = mRandomGenerator.nextInt(mDataList.size());
            if ( index == mCurrentIndex) {
                index++;
                if (index > mDataList.size())
                    index = 0;
            }
            return mDataList.get(index);
        }

        @Override
        public Data next() {
            mCurrentIndex = mRandomGenerator.nextInt(mDataList.size());
            return mDataList.get(mCurrentIndex);
        }

        @Override
        public void setCurrentAnswer(boolean correct) {}
    }

    // http://www.blueraja.com/blog/477/a-better-spaced-repetition-learning-algorithm-sm2
    public class SM2Iterator implements WordListData.ComplexIterator
    {
        private Random mRandomGenerator = new Random(System.currentTimeMillis());
        private int mCurrentDate = 0;

        private List<Data> mLocalStack = new ArrayList<>();
        private int mCurrentIndex = 0;

        private void updateLocalStack() {
            if ( mLocalStack.size() == 0 || mCurrentIndex >= mLocalStack.size() || mCurrentIndex > 20 ) {
                mLocalStack.clear();
                for(Data word : mDataList) {
                    mLocalStack.add(word);
                }

                mRandomGenerator.nextInt();
                int n = mLocalStack.size();
                for (int i = 0; i < n; i++) {
                    int change = i + mRandomGenerator.nextInt(n - i);
                    Collections.swap(mLocalStack, i, change);
                }

                Collections.sort(mLocalStack, new Comparator<Data>() {
                    @Override
                    public int compare(Data lhs, Data rhs) {
                        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                        return lhs.mPercentOverdue > rhs.mPercentOverdue ? -1 : (lhs.mPercentOverdue < rhs.mPercentOverdue) ? 1 : 0;
                    }
                });
            }
        }

        public SM2Iterator()
        {
            updateLocalStack();
        }

        @Override
        public Data get() {
            return mLocalStack.get(mCurrentIndex);
        }

        @Override
        public Data getRandomExcudingCurrent() {
            int index = mRandomGenerator.nextInt(mLocalStack.size());
            if ( index == mCurrentIndex) {
                index++;
                if (index > mLocalStack.size())
                    index = 0;
            }
            return mLocalStack.get(index);
        }

        @Override
        public WordListData.Data next() {
            this.updateLocalStack();
            WordListData.Data result = mLocalStack.get(mCurrentIndex);
            mCurrentIndex++;
            return result;
        }

        @Override
        public void setCurrentAnswer(boolean isCorrect) {
            Data data = mLocalStack.get(mCurrentIndex);
            double percentOverdue = 1;
            if ( isCorrect )
                percentOverdue = Math.min(2.0f, (mCurrentDate - data.mDateLastReviewed) / data.mDaysBetweenReviews);

            if ( data.mUses == 0 )
                data.mUses = 1;

            double performanceRating = data.mErrors / (data.mUses - data.mSkipped);
            data.mDifficulty += percentOverdue * 1/17 * (8 - 9 * performanceRating);

            if (data.mDifficulty < 0.0) data.mDifficulty = 0.0;
            else if (data.mDifficulty > 1.0f) data.mDifficulty = 1.0;

            double difficultyWeight =  3.0 - 1.7 * data.mDifficulty;

            if ( isCorrect )
                data.mDaysBetweenReviews *= 1.0 + (difficultyWeight - 1.0) * percentOverdue;
            else
                data.mDaysBetweenReviews *= difficultyWeight * difficultyWeight;

            if ( data.mDaysBetweenReviews <= 0 )
                data.mDaysBetweenReviews = 1;
        }
    }

    public class DistributionIterator implements WordListData.ComplexIterator
    {
        private DistributedRandomNumberGenerator mGenerator;
        private int mCurrentIndex;
        private int mMaxUses;

        DistributionIterator() {
            mGenerator = new DistributedRandomNumberGenerator();
            int cnt = mDataList.size();
            mMaxUses = 0;
            for(int i=0; i<cnt; i++) {
                Data data = mDataList.get(i);
                mMaxUses = Math.max(mMaxUses, data.mUses);
            }

            if ( mMaxUses == 0 )
                mMaxUses = 1;

            for(int i=0; i<cnt; i++) {
                Data data = mDataList.get(i);
                double distribution = (1 - data.mUses / mMaxUses);
                mGenerator.addNumber(i, distribution );
            }

            mCurrentIndex = mGenerator.getDistributedRandomNumber();
        }

        @Override
        public Data get() {
            return mDataList.get(mCurrentIndex);
        }

        @Override
        public Data getRandomExcudingCurrent() {
            int index = mGenerator.getDistributedRandomNumber();
            if ( index == mCurrentIndex) {
                index++;
                if (index > mDataList.size())
                    index = 0;
            }
            return mDataList.get(index);
        }

        @Override
        public Data next() {
            double distribution = mGenerator.getDistribution( mCurrentIndex );
            double uses = (distribution - 1) / mMaxUses;
            mMaxUses++;
            distribution = (1 - (uses+1) / mMaxUses);
            mGenerator.addNumber(mCurrentIndex, distribution );

            mCurrentIndex = mGenerator.getDistributedRandomNumber();
            return mDataList.get(mCurrentIndex);
        }

        @Override
        public void setCurrentAnswer(boolean correct) {}

        public class DistributedRandomNumberGenerator {

            private Map<Integer, Double> distribution;
            private double distSum;

            public DistributedRandomNumberGenerator() {
                distribution = new HashMap<>();
            }

            public double getDistribution(int value) {
                return this.distribution.get(value);
            }

            public void addNumber(int value, double distribution) {
                if (this.distribution.get(value) != null) {
                    distSum -= this.distribution.get(value);
                }
                this.distribution.put(value, distribution);
                distSum += distribution;
            }

            public int getDistributedRandomNumber() {
                double rand = Math.random();
                double ratio = 1.0f / distSum;
                double tempDist = 0;
                for (Integer i : distribution.keySet()) {
                    tempDist += distribution.get(i);
                    if (rand / ratio <= tempDist) {
                        return i;
                    }
                }
                return 0;
            }

        }
    }

    public WordListData.ComplexIterator iterator_sm2() {
        return new SM2Iterator();
    }
    public WordListData.ComplexIterator iterator_random() { return new RandomIterator(); }
    public WordListData.ComplexIterator iterator_distribution() { return new DistributionIterator(); }
}