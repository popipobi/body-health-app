package com.example.myapplication;

import android.util.Log;

import com.besthealth.bhBodyComposition120.BhBodyComposition;
import com.besthealth.bhBodyComposition120.BhErrorType;
import com.besthealth.bhBodyComposition120.BhPeopleType;
import com.besthealth.bhBodyComposition120.BhSex;
import com.holtek.libHTBodyfat.HTBodyBasicInfo;
import com.holtek.libHTBodyfat.HTBodyResultAllBody;

public class BodyFatCalculator {
    private static final String TAG = "BodyFatCalculator";

    // 用户基本信息
    private int sex; // 0=女性, 1=男性
    private int age;
    private int height; // cm
    private float weight; // kg

    // 保存最近的阻抗数据
    private int leftArmImpedance;
    private int rightArmImpedance;
    private int leftLegImpedance;
    private int rightLegImpedance;
    private int leftBodyImpedance;
    private int rightBodyImpedance;
    private int allBodyImpedance;
    private int algorithm; // 使用的算法类型

    // 是否收集完所有阻抗数据
    private boolean hasCompleteImpedanceData = false;

    // 体脂计算结果
    private BodyFatResult lastCalculatedResult;

    public BodyFatCalculator() {
        // 设置默认值
        this.sex = 1; // 默认男性
        this.age = 30;
        this.height = 170;
        this.weight = 70.0f;

        resetImpedanceData();
    }

    // 设置用户基本信息
    public void setUserInfo(int sex, int age, int height, float weight) {
        this.sex = sex;
        this.age = age;
        this.height = height;
        this.weight = weight;
        Log.d(TAG, "设置用户信息: 性别=" + sex + " 年龄=" + age + " 身高=" + height + " 体重=" + weight);
    }

    // 重置阻抗数据
    public void resetImpedanceData() {
        leftArmImpedance = -1;
        rightArmImpedance = -1;
        leftLegImpedance = -1;
        rightLegImpedance = -1;
        leftBodyImpedance = -1;
        rightBodyImpedance = -1;
        allBodyImpedance = -1;
        algorithm = -1;
        hasCompleteImpedanceData = false;
    }

    // 更新阻抗数据
    public void updateImpedance(int impedance, int part, int algorithmType) {
        algorithm = algorithmType;

        // 根据part值存储对应部位的阻抗
        switch (part) {
            case 0: // 左全身
                leftBodyImpedance = impedance;
                break;
            case 1: // 右全身
                rightBodyImpedance = impedance;
                break;
            case 2: // 左脚
                leftLegImpedance = impedance;
                break;
            case 3: // 右脚
                rightLegImpedance = impedance;
                break;
            case 4: // 左手
                leftArmImpedance = impedance;
                break;
            case 5: // 右手
                rightArmImpedance = impedance;
                break;
            case 7: // 全身
                allBodyImpedance = impedance;
                break;
        }

        // 检查是否收集了所有需要的阻抗数据
        checkImpedanceCompletion();
    }

    // 检查是否收集完所有阻抗数据
    private void checkImpedanceCompletion() {
        // 简单检查是否有足够的数据进行计算
        if (leftLegImpedance > 0 && rightLegImpedance > 0 &&
                leftArmImpedance > 0 && rightArmImpedance > 0) {
            hasCompleteImpedanceData = true;
            Log.d(TAG, "已收集完所有阻抗数据，可以进行体脂计算");
        }
    }

    // 检查是否可以计算
    public boolean canCalculate() {
        return hasCompleteImpedanceData && weight > 0;
    }

    // 使用八电极算法一计算体脂数据
    public BodyFatResult calculateBodyFatWithAlgorithm1() {
        if (!canCalculate()) {
            Log.e(TAG, "数据不完整，无法计算体脂");
            return null;
        }

        try {
            // 使用HTBodyBasicInfo和HTBodyResultAllBody进行计算
            HTBodyBasicInfo basicInfo = new HTBodyBasicInfo(sex, height, weight, age);
            basicInfo.htZLeftLegImpedance = leftLegImpedance;
            basicInfo.htZRightLegImpedance = rightLegImpedance;
            basicInfo.htZLeftArmImpedance = leftArmImpedance;
            basicInfo.htZRightArmImpedance = rightArmImpedance;

            // 如果有全身阻抗，也可以设置
            if (allBodyImpedance > 0) {
                basicInfo.htTwoLegsImpedance = allBodyImpedance;
            }

            HTBodyResultAllBody result = new HTBodyResultAllBody();
            int errorType = result.getBodyfatWithBasicInfo(basicInfo);

            if (errorType == HTBodyBasicInfo.ErrorNone) {
                // 计算成功，创建结果对象
                BodyFatResult bodyFatResult = new BodyFatResult();
                bodyFatResult.bmi = result.htBMI;
                bodyFatResult.bodyFatRate = result.htBodyfatPercentage;
                bodyFatResult.bodyFatMass = result.htBodyfatKg;
                bodyFatResult.waterRate = result.htWaterPercentage;
                bodyFatResult.proteinRate = result.htProteinPercentage;
                bodyFatResult.muscleRate = result.htMusclePercentage;
                bodyFatResult.muscleMass = result.htMuscleKg;
                bodyFatResult.boneMass = result.htBoneKg;
                bodyFatResult.visceralFat = result.htVFAL;
                bodyFatResult.bmr = result.htBMR;
                bodyFatResult.bodyAge = result.htBodyAge;
                bodyFatResult.idealWeight = result.htIdealWeightKg;

                // 保存最近计算结果
                lastCalculatedResult = bodyFatResult;

                Log.d(TAG, "体脂计算成功: BMI=" + bodyFatResult.bmi +
                        " 体脂率=" + bodyFatResult.bodyFatRate + "% " +
                        " 体脂量=" + bodyFatResult.bodyFatMass + "kg");

                return bodyFatResult;
            } else {
                Log.e(TAG, "体脂计算失败，错误代码: " + errorType);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "计算体脂时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 使用八电极算法二计算体脂数据 (120kHz)
    public BodyFatResult calculateBodyFatWithAlgorithm2() {
        if (!canCalculate()) {
            Log.e(TAG, "数据不完整，无法计算体脂");
            return null;
        }

        try {
            // 使用BhBodyComposition进行计算
            BhBodyComposition bhBodyComposition = new BhBodyComposition();
            bhBodyComposition.bhSex = sex == 1 ? BhSex.MALE.ordinal() : BhSex.FEMALE.ordinal();
            bhBodyComposition.bhPeopleType = BhPeopleType.NORMAL.ordinal(); // 普通人群
            bhBodyComposition.bhWeightKg = weight;
            bhBodyComposition.bhAge = age;
            bhBodyComposition.bhHeightCm = height;

            // 设置阻抗值
            bhBodyComposition.bhZLeftArmEnCode = leftArmImpedance;
            bhBodyComposition.bhZRightArmEnCode = rightArmImpedance;
            bhBodyComposition.bhZLeftLegEnCode = leftLegImpedance;
            bhBodyComposition.bhZRightLegEnCode = rightLegImpedance;

            // 设置左全身阻抗
            if (leftBodyImpedance > 0) {
                bhBodyComposition.bhZLeftBodyEnCode = leftBodyImpedance;
            } else if (rightBodyImpedance > 0) {
                bhBodyComposition.bhZLeftBodyEnCode = rightBodyImpedance; // 没有左全身就用右全身
            }

            BhErrorType bhErrorType = BhErrorType.values()[bhBodyComposition.getBodyComposition()];

            if (bhErrorType == BhErrorType.NONE) {
                // 计算成功，创建结果对象
                BodyFatResult bodyFatResult = new BodyFatResult();
                bodyFatResult.bmi = bhBodyComposition.bhBMI;
                bodyFatResult.bodyFatRate = bhBodyComposition.bhBodyFatRate;
                bodyFatResult.bodyFatMass = bhBodyComposition.bhBodyFatKg;
                bodyFatResult.waterRate = bhBodyComposition.bhWaterRate;
                bodyFatResult.proteinRate = bhBodyComposition.bhProteinRate;
                bodyFatResult.muscleRate = bhBodyComposition.bhMuscleRate;
                bodyFatResult.muscleMass = bhBodyComposition.bhMuscleKg;
                bodyFatResult.boneMass = bhBodyComposition.bhBoneKg;
                bodyFatResult.visceralFat = bhBodyComposition.bhVFAL;
                bodyFatResult.bmr = bhBodyComposition.bhBMR;
                bodyFatResult.bodyAge = bhBodyComposition.bhBodyAge;
                bodyFatResult.idealWeight = bhBodyComposition.bhIdealWeightKg;
                bodyFatResult.skeletalMuscleMass = bhBodyComposition.bhSkeletalMuscleKg;

                // 保存最近计算结果
                lastCalculatedResult = bodyFatResult;

                Log.d(TAG, "体脂计算成功 (算法2): BMI=" + bodyFatResult.bmi +
                        " 体脂率=" + bodyFatResult.bodyFatRate + "% " +
                        " 体脂量=" + bodyFatResult.bodyFatMass + "kg");

                return bodyFatResult;
            } else {
                Log.e(TAG, "体脂计算失败，错误类型: " + bhErrorType.name());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "计算体脂时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 计算体脂，根据算法类型选择合适的算法
    public BodyFatResult calculateBodyFat() {
        if (algorithm == 1) {
            return calculateBodyFatWithAlgorithm2(); // 算法类型1对应SDK中的算法二
        } else {
            return calculateBodyFatWithAlgorithm1(); // 默认使用算法一
        }
    }

    // 获取最近计算的结果
    public BodyFatResult getLastCalculatedResult() {
        return lastCalculatedResult;
    }

    // 体脂计算结果类
    public static class BodyFatResult {
        public double bmi;                // 身体质量指数
        public double bodyFatRate;        // 体脂率(%)
        public double bodyFatMass;        // 脂肪量(kg)
        public double waterRate;          // 水分率(%)
        public double proteinRate;        // 蛋白质率(%)
        public double muscleRate;         // 肌肉率(%)
        public double muscleMass;         // 肌肉量(kg)
        public double boneMass;           // 骨量(kg)
        public int visceralFat;          // 内脏脂肪等级
        public int bmr;                  // 基础代谢(kcal)
        public int bodyAge;              // 身体年龄
        public double idealWeight;        // 理想体重(kg)
        public double skeletalMuscleMass; // 骨骼肌量(kg)
    }
}