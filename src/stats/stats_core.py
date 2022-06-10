# Copyright 2022 Suzhou Yifei YuYue Co.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import numpy as np
import scipy as sp
import scipy.stats as stats
from model import ChartData, ResponseData


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
            loc, scale = stats.norm.fit(xs)
            n = stats.norm(loc=loc, scale=scale)
            test_res = stats.kstest(xs, n.cdf)
            print(test_res)
            row.append("Kolmogorov-Smirnov检验")
        else:
            test_res = stats.shapiro(xs)
            row.append("Shapiro-Wilk检验")
        if test_res.pvalue == 0:
            row.append("不可检验")
        elif test_res.pvalue <= 0.05:
            row.append("否")
        else:
            row.append("是")
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
        x = list(zip(sorted(xs), sorted(ys)))
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
