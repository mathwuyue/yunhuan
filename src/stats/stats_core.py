import numpy as np
import scipy as sp
import pandas as pd
import matplotlib.pyplot as plt
import scipy.stats as stats
from model import ResponseData, ChartData

# lightgbm
import lightgbm as lgb
from sklearn.pipeline import Pipeline
from imblearn.over_sampling import SMOTE
from sklearn.impute import SimpleImputer
from sklearn.pipeline import FeatureUnion
from sklearn.metrics import roc_auc_score, f1_score, classification_report, roc_curve,accuracy_score, average_precision_score,precision_score,recall_score
from sklearn.model_selection import GridSearchCV,train_test_split
from sklearn_features.transformers import DataFrameSelector
from sklearn.preprocessing import OneHotEncoder, StandardScaler



def parse_input(request):
    """Function to parse gRPC input to Python data
    - Return (xarray, yarray)
     - xarray: [[1,2,3], [2,3,4]...], for different Xs, each X is an array of numbers. Or None
     - yarray: similar to xarray or None
    """
    return (request.x, request.y)


def cal_basic_statistics(request):
    """
    The module is to calculate the basic statistics summarize of the data
    """
    # extract input data
    xarrays, _ = parse_input(request)
    trows = []
    boxplots = []
    for xs in xarrays:
        # build table data
        xmean = np.mean(xs)
        xstd = np.std(xs)
        xmin = np.min(xs)
        xmax = np.max(xs)
        x25 = np.percentile(xs, 25)
        x50 = np.percentile(xs, 50)
        x75 = np.percentile(xs, 75)
        trows.append(
            [
                "%.2f" % xmean,
                "%.2f" % xstd,
                "%.2f" % xmin,
                "%.2f" % x25,
                "%.2f" % x50,
                "%.2f" % x75,
                "%.2f" % xmax,
            ]
        )
        # boxplot data
        boxplots.append([xmin, x25, x50, x75, xmax])
    # compose table data
    chart1 = ChartData(
        chartId=0, yaxis=["均值", "标准差", "最小值", "25分位值", "50分位值", "75分位值", "最大值"], y=trows
    )
    # compose boxplot data
    chart2 = ChartData(chartId=4, y=boxplots)
    # composite result data
    result = ResponseData(total=2, info="ok", chartArray=[chart1, chart2])
    return result


def cal_test_of_normality(request):
    """
    The module is to test whether the data follows normal distribution.
    - Calculate skewness and kurtosis (偏度和峰度), calculate the Sharpiro-Wilk Test if the size of data
      set is less then 5000, otherwise calculate the Kolmogorov Test.
    - Give the judgement based on the result
    - cdf to draw the CDF plot of data
    Input:  request as stats.proto.ComputeRequest
    Return: chart_id = 0 for table. Each dataset has a chart_id=2 plot data
    """
    xarrays, _ = parse_input(request)
    is_norm = []
    qqplot = []
    histogramplot = []
    chartArray = []
    for xs in xarrays:
        row = []
        skew = stats.skew(xs)
        kurtosis = stats.kurtosis(xs)
        if len(xs) >= 2000:
            test_res = stats.kstest(xs, "norm")
            row.append("Kolmogorov-Smirnov检验")
        else:
            test_res = stats.shapiro(xs)
            row.append("Shapiro-Wilk检验")
        if test_res.pvalue == 0:
            row.append("不可检验")
        elif test_res.pvalue <= 0.05:
            row.append("是")
        else:
            row.append("否")
        row.append(str(len(xs)))
        row.append("%.2f" % skew)
        row.append("%.2f" % kurtosis)
        row.append("%.2f" % test_res.pvalue)
        is_norm.append(row)
        # histogram
        hist, edges = np.histogram(xs, bins=36, density=True)
        hist_chart = ChartData(chartId=3, x=[edges.tolist()], y=[hist.tolist()])
        histogramplot.append(hist_chart)
        # qq-plot
        m = np.mean(xs)
        st = np.std(xs)
        xsn = (np.array(xs) - m) / st
        q = np.quantile(xsn, [i / len(xsn) for i in range(1, len(xsn) + 1)]).tolist()
        qq_chart = ChartData(
            chartId=10,
            x=list(zip(q, sorted(xsn.tolist()))),
            y=[[q[0], q[0]], [q[-1], q[-1]]],
        )
        qqplot.append(qq_chart)
    # compose table data
    chart1 = ChartData(
        chartId=0, yaxis=["检验方法", "是否正态分布", "样本数", "偏度", "峰度", "p-value"], y=is_norm
    )
    chartArray.append(chart1)
    # compose Histogram data
    chartArray.extend(histogramplot)
    # compose Q-Q plot data
    chartArray.extend(qqplot)
    # compose result data
    result = ResponseData(total=1 + 2 * len(xarrays), info="ok", chartArray=chartArray)
    return result


def cal_test_relative(request):
    """
    The module calculate
    https://zhuanlan.zhihu.com/p/343361192
    """
    xarray, yarray = parse_input(request)
    # table array
    table_rows = []
    row_format = "相关系数：{r:.2f}, p-value={p:.2f}"
    # scatter plots
    scatter_plots = []
    # result arrays
    chartArray = []
    for xs, ys in zip(xarray, yarray):
        row = []
        if len(xs) != len(ys):
            table_rows.append(["N/A", "N/A", "N/A", "N/A", "数据数量不同，无法计算"])
            continue
        if stats.normaltest(xs).pvalue <= 0.05 and stats.normaltest(ys).pvalue <= 0.05:
            pearsonr, pvalue = stats.pearsonr(xs, ys)
            row.append(row_format.format(r=pearsonr, p=pvalue))
        else:
            pearsonr = -2
        # spearman
        spearmanr = stats.spearmanr(xs, ys)
        row.append(row_format.format(r=spearmanr.correlation, p=spearmanr.pvalue))
        # kendal tau
        kendaltau = stats.kendalltau(xs, ys)
        row.append(row_format.format(r=kendaltau.correlation, p=kendaltau.pvalue))
        if pearsonr >= -1:
            if pvalue <= 0.05:
                if pearsonr >= 0.5:
                    row.extend(["显著正线性相关", "线性相关性强"])
                elif pearsonr <= -0.5:
                    row.extend(["显著负线性相关", "线性相关性强"])
                elif spearmanr.pvalue <= 0.05:
                    if spearmanr.correlation >= 0.5:
                        row.extend(["显著正相关", "相关性强，但非线性相关"])
                    elif spearmanr.correlation <= -0.5:
                        row.extend(["显著负相关", "相关性强，但非线性相关"])
                    elif pearsonr >= 0:
                        row.extend(["显著正线性相关", "相关性弱"])
                    else:
                        row.extend(["显著负线性相关", "相关性弱"])
                elif pearsonr >= 0:
                    row.extend(["显著正线性相关", "相关性弱"])
                else:
                    row.extend(["显著负线性相关", "相关性弱"])
            elif spearmanr.pvalue <= 0.05:
                if spearmanr.correlation >= 0.5:
                    row.extend(["显著正相关", "相关性强"])
                elif spearmanr.correlation <= -0.5:
                    row.extend(["显著负相关", "相关性强"])
                elif spearmanr.correlation >= 0:
                    row.extend(["显著正相关", "相关性弱"])
                else:
                    row.extend(["显著负相关", "相关性弱"])
            else:
                row.extend(["N/A", "相关性不显著"])
        else:
            if spearmanr.pvalue <= 0.05:
                if spearmanr.correlation >= 0.5:
                    row.extend(["显著正相关", "相关性强"])
                elif spearmanr.correlation <= -0.5:
                    row.extend(["显著负相关", "相关性强"])
                elif spearmanr.correlation >= 0:
                    row.extend(["显著正相关", "相关性弱"])
                else:
                    row.extend(["显著负相关", "相关性弱"])
            else:
                row.extend(["N/A", "相关性不显著"])
        # put into table
        table_rows.append(row)
        # scartter plot
        scatter_plots.append(
            ChartData(
                chartId=10,
                x=list(zip(sorted(xs), sorted(ys))),
                y=[(np.min(xs), np.min(ys)), (np.max(xs), np.max(ys))],
            )
        )
    # table data
    chart1 = ChartData(
        chartId=0,
        yaxis=["Pearson检验", "Spearman检验", "Kendall检验", "相关性结果", "备注"],
        y=table_rows,
    )
    chartArray.append(chart1)
    chartArray.extend(scatter_plots)
    # compose result data
    result = ResponseData(total=1 + len(xarray), info="ok", chartArray=chartArray)
    return result


def cal_significant_test(request):
    """
    https://baike.baidu.com/item/显著性/10211648
    """
    xarray, yarray = parse_input(request)


def cal_svm(request):
    xarray, yarray = parse_input(request)


def cal_logistic(request):
    xarray, yarray = parse_input(request)


def cal_lightgbm(request):
    xarray, yarray = parse_input(request)
    X = pd.DataFrame(xarray)
    Y = pd.DataFrame(yarray)
    X_train, X_test, y_train, y_test = train_test_split(X, Y, test_size=0.2)

    """数据预处理"""
    num_attribs = list(X_train)
    cat_attribs = list(y_train)
    # 数据处理流水线
    num_pipeline = Pipeline([
            ('selector', DataFrameSelector(num_attribs)), # 挑出数值属性
            ('imputer', SimpleImputer()),   # 填充缺失值
            ('std_scaler', StandardScaler()) # 特征缩放
        ])

    cat_pipeline = Pipeline([
            ('selector',DataFrameSelector(cat_attribs)), # 挑出分类属性
            ('imputer', SimpleImputer(strategy='most_frequent')),   # 填充缺失值
            ('encoder', OneHotEncoder()) # 独热编码
        ])

    full_pipeline = FeatureUnion(transformer_list=[
            ('num_pipeline', num_pipeline),
            ('cat_pipeline', cat_pipeline)
        ])
    X_train_prepared = full_pipeline.fit_transform(X_train)
    X_test_prepared = full_pipeline.fit_transform(X_test)
    # 处理样本不平衡，降低样本近邻个数，提了几个点精确度，避免小样本近邻插值报错
    oversample = SMOTE(k_neighbors=2)
    X_train_prepared_smote, y_train_smote = oversample.fit_resample(X_train_prepared, y_train)
    # print(type(X_train_prepared_smote))
    # print(type(y_train_smote))
    """模型训练"""
    clf = lgb.LGBMClassifier()
    # 网格搜索，寻找最优参数组合
    params = {'max_depth': [3, 5 ,6, 9],
            'learning_rate': [0.1,0.15,0.3],
            'num_leaves': [10, 20, 30, 40, 50, 60],
            'is_unbalance': [True]
            }

    grid_search = GridSearchCV(clf, param_grid=params, scoring='f1_weighted', cv=3, n_jobs=-1)
    grid_search.fit(X_train_prepared_smote, y_train_smote)
    best_clf = grid_search.best_estimator_ # 最佳分类器
    # print(best_clf)
    
    # 保存模型
    best_clf.booster_.save_model("lgb.txt")
    # 加载模型
    # clf_loads = lgb.Booster(model_file='lgb.txt')
    # probas  = clf_fs.predict(test)
    
    # 保存feature_importance
    booster = best_clf.booster_
    importance = booster.feature_importance(importance_type='split')
    # print(importance)
    # feature_name = booster.feature_name()
    # feature_importance = pd.DataFrame({
    #     'feature_name':feature_name,'importance':importance} )
    # print(feature_importance)
    # chart_fi = ChartData(
    #     chartId=0,
    #     yaxis=feature_name,
    #     y=importance,
    # )
    """模型预测与评估"""
    y_pred = best_clf.predict(X_test_prepared) # 使用最优模型进行预测
    
    chartArray = []
    chart1 = ChartData(
        chartId = 0, yaxis=["Weighted precision", "Weighted recall", "Weighted f1-score"], 
        y=[[precision_score(y_test, y_pred, average='weighted'), recall_score(y_test, y_pred, average='weighted'), f1_score(y_test, y_pred, average='weighted')]]
    )
    chartArray.append(chart1)
    chart2 = ChartData(
        chartId = 0, yaxis=["Macro precision", "Macro recall", "Macro f1-score"], 
        y=[[precision_score(y_test, y_pred, average='macro'), recall_score(y_test, y_pred, average='macro'), f1_score(y_test, y_pred, average='macro')]]
    )
    chartArray.append(chart2)
    chart3 = ChartData(
        chartId = 0, yaxis=["Micro precision", "Micro recall", "Micro f1-score"], 
        y=[[precision_score(y_test, y_pred, average='micro'), recall_score(y_test, y_pred, average='micro'), f1_score(y_test, y_pred, average='micro')]]
    )
    chartArray.append(chart3)
    chart4 = ChartData(
        chartId = 0, yaxis=["roc_auc"], 
        y=[[roc_auc_score(y_test, y_pred, multi_class="ovo")]]
    )
    chartArray.append(chart4)
    
    # 因为这是multiclass，很难画图，后面再想办法吧
    # y_pred = best_clf.predict_proba(X_test_prepared)
    # fpr, tpr, thersholds = roc_curve(y_test, y_pre, pos_label=2)
    # roc_auc = auc(fpr, tpr)
    
    # plt.plot(fpr, tpr, 'k--', label='ROC (area = {0:.2f})'.format(roc_auc), lw=2)
    
    # plt.xlim([-0.05, 1.05])  # 设置x、y轴的上下限，以免和边缘重合，更好的观察图像的整体
    # plt.ylim([-0.05, 1.05])
    # plt.xlabel('False Positive Rate')
    # plt.ylabel('True Positive Rate')  # 可以使用中文，但需要导入一些库即字体
    # plt.title('ROC Curve')
    # plt.legend(loc="lower right")
    # plt.show()
    # info 里面是重要性
    return ResponseData(total=1, info=str(list(importance)), chartArray=chartArray)

