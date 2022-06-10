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

from itertools import cycle

import lightgbm as lgb
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from model import ChartData, ResponseData

# Importing the necessary packages and libaries
from sklearn import datasets, svm
from sklearn.metrics import (
    accuracy_score,
    auc,
    average_precision_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
    roc_curve,
)
from sklearn.model_selection import GridSearchCV, train_test_split
from sklearn.multiclass import OneVsRestClassifier
from sklearn.preprocessing import label_binarize


class DataError(Exception):
    def __init__(self, message) -> None:
        super().__init__(message)
        self.resp = ResponseData(total=-1, info=message)


def parse_input(request):
    """Function to parse gRPC input to Python data
    - Return (xarray, yarray)
     - xarray: [[1,2,3], [2,3,4]...], for different Xs, each X is an array of numbers. Or None
     - yarray: similar to xarray or None
    """
    return (request.x, request.y)


def cal_category(xs):
    """
    keys are the categories of the variable
    """
    category = {}
    for x in xs:
        if x in category.keys():
            category[x] = category[x] + 1
        else:
            category[x] = 0
    return list(category.keys())


def cal_roc_curve(y_classes, y_test, y_score):
    n_classes = len(y_classes)
    fpr = dict()
    tpr = dict()
    roc_auc = dict()
    # ChartData x
    chart_x = []
    # Chart Data y
    chart_y = []
    # yaxis
    yaxis = []
    for i in range(n_classes):
        fpr[i], tpr[i], _ = roc_curve(y_test[:, i], y_score[:, i])
        roc_auc[i] = auc(fpr[i], tpr[i])
        chart_x.append(fpr[i].tolist())
        chart_y.append(tpr[i].tolist())
        yaxis.append(
            "ROC curve of class {0} (area = {1:0.2f})".format(y_classes[i], roc_auc[i])
        )
    # Compute micro-average ROC curve and ROC area
    fpr["micro"], tpr["micro"], _ = roc_curve(y_test.ravel(), y_score.ravel())
    roc_auc["micro"] = auc(fpr["micro"], tpr["micro"])
    chart_x.append(fpr["micro"].tolist())
    chart_y.append(tpr["micro"].tolist())
    yaxis.append("micro-average ROC curve (area = {0:0.2f})".format(roc_auc["micro"]))
    # First aggregate all false positive rates
    all_fpr = np.unique(np.concatenate([fpr[i] for i in range(n_classes)]))
    # Then interpolate all ROC curves at this points
    mean_tpr = np.zeros_like(all_fpr)
    for i in range(n_classes):
        print(fpr[i])
        print(np.interp(all_fpr, fpr[i], tpr[i]))
        mean_tpr += np.interp(all_fpr, fpr[i], tpr[i])
    # Finally average it and compute AUC
    mean_tpr /= n_classes
    print("n_classes: ", n_classes)
    # Compute macro-average ROC curve and ROC area
    fpr["macro"] = all_fpr
    tpr["macro"] = mean_tpr
    roc_auc["macro"] = auc(fpr["macro"], tpr["macro"])
    chart_x.append(fpr["macro"].tolist())
    chart_y.append(tpr["macro"].tolist())
    yaxis.append("macro-average ROC curve (area = {0:0.2f})".format(roc_auc["macro"]))
    # ChartData
    return ChartData(chartId=1, yaxis=yaxis, x=chart_x, y=chart_y)


def prepare_data(xarrays, yarrays):
    y = yarrays[0]
    if len(xarrays[0]) != len(y):
        raise DataError("x, y数据维度不同，无法计算")
    # check x 维度是否相同
    try:
        pd.DataFrame(xarrays, columns=[str(i) for i in range(len(xarrays[0]))])
    except ValueError:
        raise DataError("x数据维度不同，无法计算")
    # prepare data
    X = np.transpose(np.array(xarrays))
    y_classes = cal_category(y)
    y = label_binarize(y, classes=y_classes)
    return (X, y, y_classes)


def cal_svm(request):
    # return charts
    charts = []
    xarrays, yarrays = parse_input(request)
    try:
        X, y, y_classes = prepare_data(xarrays, yarrays)
    except DataError as e:
        return e.resp
    # train and test set
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, train_size=0.8, random_state=0
    )
    # random state
    random_state = np.random.RandomState(0)
    # svm different kernel
    linear = OneVsRestClassifier(
        svm.SVC(kernel="linear", probability=True, random_state=random_state)
    )
    rbf = OneVsRestClassifier(
        svm.SVC(kernel="rbf", probability=True, random_state=random_state)
    )
    poly = OneVsRestClassifier(
        svm.SVC(kernel="poly", degree=3, probability=True, random_state=random_state)
    )
    sig = OneVsRestClassifier(
        svm.SVC(kernel="sigmoid", probability=True, random_state=random_state)
    )
    classifiers = {"linear": linear, "rbf": rbf, "poly": poly, "sig": sig}
    # fit
    linear_score = linear.fit(X_train, y_train).decision_function(X_test)
    rbf_score = rbf.fit(X_train, y_train).decision_function(X_test)
    poly_score = poly.fit(X_train, y_train).decision_function(X_test)
    sig_score = sig.fit(X_train, y_train).decision_function(X_test)
    fits = [linear_score, rbf_score, poly_score, sig_score]
    # roc_auc_score
    yaxis = ["SVM核", "准确率", "ROC_AUC（Macro）", "ROC_AUC（Weighted）"]
    y_data = []
    cm_data = {}
    for name, classifier in classifiers.items():
        y_prob = classifier.predict_proba(X_test)
        try:
            macro_roc_auc_ovr = roc_auc_score(
                y_test, y_prob, multi_class="ovr", average="macro"
            )
        except ValueError:
            macro_roc_auc_ovr = -1
        try:
            weighted_roc_auc_ovr = roc_auc_score(
                y_test, y_prob, multi_class="ovr", average="weighted"
            )
        except ValueError:
            weighted_roc_auc_ovr = -1
        # get label
        y_pred = np.argmax(y_prob, axis=1)
        y_label = np.argmax(y_test, axis=1)
        # accuracy
        accuracy = accuracy_score(y_label, y_pred)
        y_data.append(
            [
                name,
                "{:.2f}".format(accuracy),
                "{:.2f}".format(macro_roc_auc_ovr),
                "{:.2f}".format(weighted_roc_auc_ovr),
            ]
        )
        # confusion matrix
        cm_data[name] = confusion_matrix(y_label, y_pred)
    # table chart
    charts.append(ChartData(chartId=0, yaxis=yaxis, y=y_data))
    # confusion matrix chart data
    for cm in cm_data.values():
        charts.append(
            ChartData(chartId=16, xaxis=y_classes, yaxis=y_classes, x=cm.tolist())
        )
    # roc plots
    for y_score in fits:
        roc_curve_chart = cal_roc_curve(y_classes, y_test, y_score)
        # ChartData
        charts.append(roc_curve_chart)
    # return everything
    return ResponseData(total=9, info="ok", chartArray=charts)


def cal_lightgbm(request):
    # get params
    xarrays, yarrays = parse_input(request)
    # return charts
    charts = []
    try:
        # prepare data
        X, y, y_classes = prepare_data(xarrays, yarrays)
    except DataError as e:
        return e.resp
    # train and test set
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, train_size=0.8, random_state=666
    )
    # LGB classifier
    clf = lgb.LGBMClassifier()
    # grid search
    # 网格搜索，寻找最优参数组合
    params = {
        "max_depth": [3, 5, 6, 9],
        "learning_rate": [0.1, 0.15, 0.3],
        "num_leaves": [10, 20, 30, 40, 50, 60],
        "is_unbalance": [True],
    }
    grid_search = GridSearchCV(
        clf, param_grid=params, scoring="accuracy", cv=3, n_jobs=-1
    )
    grid_search.fit(X_train, y_train)
    # get best classifier
    best_clf = grid_search.best_estimator_
    # pred
    y_pred = best_clf.predict(X_test)
