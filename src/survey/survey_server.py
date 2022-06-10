# TODO: SQL is too lousy

import os

import gevent
from flask.globals import session
from gevent import monkey
from gevent.pywsgi import WSGIServer

monkey.patch_all()

import datetime
import json

import psycopg2
import redis
from alibaba.sms import SMSVerfication
from decorator import login_required
from flask import Flask as _Flask
from flask import jsonify, request
from flask.json import JSONEncoder as _JSONEncoder
from flask_cors import *
from gevent.pywsgi import WSGIServer
from psycopg2 import pool
from utils import *
from werkzeug.utils import secure_filename

app = _Flask(__name__, static_url_path="")
app.secret_key = (
    "481cb13fd469059b2dbf88d8c2b3061d2be4db40fe5448d4c23ee0e195c7f572"  # 设置session秘钥
)
PERMANENT_SESSION_LIFETIME = datetime.timedelta(days=1)  # 设置session过期时间
UPLOAD_FOLDER = "tmp"
app.config["SESSION_COOKIE_HTTPONLY"] = False  # 更改httponly 为 False
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER
app.token = "d926d70400073b24d77be82f8395abd4"

ps_pool = psycopg2.pool.SimpleConnectionPool(
    1, 1000, user="survey", password="survey@2021", database="survey"
)
redis_pool = redis.ConnectionPool(host="localhost", port=6379, db=0)
# ps_pool = psycopg2.pool.SimpleConnectionPool(1, 1000, database="survey", user="postgres", password="123456", host="127.0.0.1", port="5432")


CORS(app, resources={r"/*": {"origins": "*"}}, supports_credentials=True)


class JSONEncoder(_JSONEncoder):
    def default(self, o):
        import decimal

        if isinstance(o, decimal.Decimal):

            return float(o)

        super(JSONEncoder, self).default(o)


class Flask(_Flask):
    json_encoder = JSONEncoder


class DB:
    def __init__(self, pg_pool):
        self.pg_pool = pg_pool

    def start_db(self):
        self.ps_conn = self.pg_pool.getconn()
        while not self.ps_conn:
            gevent.sleep(1)
            ps_conn = self.pg_pool.getconn()
        cur = self.ps_conn.cursor()
        return cur

    def finish_db(self, cur):
        cur.close()
        self.ps_conn.commit()
        self.pg_pool.putconn(self.ps_conn)


class Enum:
    def __init__(self):
        self.hosipital = ""
        self.city = ""
        self.province = ""

    def hosipitalList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select id,name from hosipital")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def cityList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select id,name from city")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def provinceList(self):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            curs.execute("select pid,name from province")
            list_header = [row[0] for row in curs.description]
            list_result = [[str(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
            return res

    def hosipital_name(self, num):
        """
        医院列表
        :param num:医院对应id
        :return:医院名字
        """
        for i in self.hosipital:
            if int(i["id"]) == num:
                return i["name"]
        return ""

    def hosipitalId(self, name):
        for i in self.hosipital:
            if i["name"] == name:
                return i["id"]
        return 0

    def cityId(self, name):
        for i in self.city:
            if i["name"] == name:
                return i["id"]
        return 0

    def cityName(self, id):
        for i in self.city:
            if i["id"] == id:
                return i["name"]
        return ""

    def provinceId(self, name):
        for i in self.province:
            if i["name"] == name:
                return i["pid"]
        return 0

    def provinceName(self, id):
        for i in self.province:
            if i["pid"] == id:
                return i["name"]
        return ""

    def hosipitalCount(self, id=None):
        db = DB(ps_pool)
        curs = db.start_db()
        try:
            sql = "select hosipital,count(*) from case_info group by hosipital"
            if id:
                sql = "select hosipital,count(*) from case_info group by hosipital having hosipital={}".format(
                    id
                )
            curs.execute(sql)
            list_header = [row[0] for row in curs.description]
            list_result = [[int(item) for item in row] for row in curs.fetchall()]
            res = [dict(zip(list_header, row)) for row in list_result]
            if not res:
                res = [{"hosipital": id, "count": 0}]
            return res
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)

    def doctorLevel(self, num):
        """
        医生级别列表
        :param num:
        :return:
        """
        dict = {
            100: "专家",
            11: "普通医生",
            10: "普通医生（非会员）",
            0: "基层人员",
        }
        return dict[num]


def success(data="success"):
    res = {"status": 1, "resp": data}
    return jsonify(res), 200


def fail(status=-1, error="系统异常！请稍后重试！", http_code=200):
    res = {"status": status, "resp": error}
    return jsonify(res), http_code


# 医护人员信息


@app.route("/userinfo", methods=["POST", "GET"])
@login_required(10)
def user():
    # 添加
    db = DB(ps_pool)
    curs = db.start_db()
    if request.method == "POST":
        data = json.loads(request.get_data(as_text=True))
        # 校验信息是否已录入
        try:

            sql = "insert into doctor(username,email,sex,phone_number,hosipital,department) values('{username}','{email}','{sex}','{phone_number}',{hosipital},'{department}') on conflict(phone_number) do update set \
                username='{username}',email='{email}',sex='{sex}',phone_number='{phone_number}',hosipital={hosipital},department='{department}',is_valid=true RETURNING id".format(
                **data
            )
            curs.execute(sql)
            res = curs.fetchone()[0]
            return success({"id": res, "msg": "success"})
        except (psycopg2.DatabaseError, KeyError) as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 获取
    if request.method == "GET":
        enum = Enum()
        enum.hosipital = enum.hosipitalList()
        data = request.args.to_dict()
        sql = "select id,username,email,sex,phone_number,hosipital,department,num_patients from doctor where is_valid=true"
        if "hosipital" in data.keys():
            sql = "select id,username,email,sex,phone_number,hosipital,department,num_patients from doctor where is_valid=true and hosipital={}".format(
                data["hosipital"]
            )
        try:
            res = curs.execute(sql)
            u = curs.fetchall()
            if not u:
                return success("")
            payload = []
            content = {}
            for i in u:
                content = {
                    "id": i[0],
                    "username": i[1],
                    "email": i[2],
                    "sex": i[3],
                    "phone_number": i[4],
                    "hosipital": enum.hosipital_name(i[5]),
                    "hosipital_id": i[5],
                    "department": i[6],
                    "num_patients": i[7],
                }
                payload.append(content)
            return success(payload)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 删除


@app.route("/userinfo/<id>", methods=["PUT", "GET", "DELETE"])
@login_required(10)
def userInfo(id):
    db = DB(ps_pool)
    enum = Enum()
    enum.hosipital = enum.hosipitalList()
    enum.city = enum.cityList()
    enum.province = enum.provinceList()
    curs = db.start_db()
    if request.method == "GET":
        try:
            res = curs.execute(
                "select id,username,email,sex,phone_number,hosipital,department,num_patients from doctor where is_valid=true and id='{id}'".format(
                    id=id
                )
            )
            get = curs.fetchall()
            if not get:
                return success("")
            data = get[0]
            content = {
                "id": data[0],
                "username": data[1],
                "email": data[2],
                "sex": data[3],
                "phone_number": data[4],
                "hosipital": enum.hosipital_name(data[5]),
                "hosipital_id": data[5],
                "department": data[6],
                "num_patients": data[7],
            }
            return success(content)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 修改
    if request.method == "PUT":
        data = json.loads(request.get_data(as_text=True))
        data["id"] = id
        # data['province']=enum.provinceId(data['province'])
        # data['city']=enum.cityId(data['city'])
        sql = "update doctor set username='{username}',email='{email}',sex='{sex}',phone_number='{phone_number}',hosipital='{hosipital}',department='{department}' where id='{id}' RETURNING id".format(
            **data
        )
        try:
            curs.execute(sql)
            res = curs.fetchone()[0]
            return success({"id": res, "msg": "success"})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    if request.method == "DELETE":
        try:
            sql = "select is_valid from doctor where id={}".format(id)
            curs.execute(sql)
            data = curs.fetchall()
            if not data:
                return fail(status=0, error="数据不存在")
            if not data[0][0]:
                return fail(status=0, error="数据已删除")
            sql = "update doctor set is_valid=False where id=%s" % (id)
            res = curs.execute(sql)
            # sql="delete from table where id = %d" %(id)
            return success()
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/caseinfo", methods=["POST", "GET"])
@login_required(0)
def case():
    db = DB(ps_pool)
    curs = db.start_db()
    # 添加
    if request.method == "POST":
        if "file" in request.files:
            file = request.files["file"]
            if file and allowed_file(file.filename):
                filename = secure_filename(file.filename)
                file.save(os.path.join(app.config["UPLOAD_FOLDER"], filename))
            try:
                save_user = request.form.get("save_user")
                curs.execute(
                    "select hosipital from doctor where phone_number=%s", (save_user,)
                )
                hosipital = curs.fetchone()[0]
                n_cases, resp = handle_case_upload(
                    os.path.join(app.config["UPLOAD_FOLDER"], filename),
                    save_user,
                    hosipital,
                    "case_info",
                    curs,
                )
                curs.execute(
                    "update doctor set num_patients = num_patients+%s where phone_number=%s",
                    (n_cases, save_user),
                )
                curs.execute(
                    "update hosipital set num_cases = num_cases+%s where id=%s",
                    (n_cases, hosipital),
                )
                return jsonify(resp)
            except psycopg2.DatabaseError as error:
                print(error)
                return jsonify({"status": -1, "resp": "请下载模板进行填写，并根据说明校对字段!"})
            finally:
                db.finish_db(curs)
        data = request.get_json()
        # data['save_time'] = datetime.datetime.now().strftime('%Y-%m-%d')
        data["bmi"] = cal_bmi(data["height"], data["weight"])
        data["tg"] = -1
        if data.get("is_draft"):
            if not data["birthday"]:
                data["birthday"] = "1900-06-08"
            curs.execute(
                "delete from case_info_draft where save_user=%s", (data["save_user"],)
            )
            try:
                sql = gen_case_insert_sql("case_info_draft", data)
            except KeyError as error:
                return str(error), 500
        else:
            if not data["birthday"]:
                data["birthday"] = "1900-06-08"
            sql = gen_case_insert_sql("case_info", data)
        try:
            curs.execute(sql)
            # 添加管理员
            res = curs.fetchone()[0]
            if not data.get("is_draft"):
                curs.execute(
                    "update doctor set num_patients = num_patients+1 where phone_number=%s",
                    (data["save_user"],),
                )
                curs.execute(
                    "update hosipital set num_cases = num_cases+1 where id=%s",
                    (data["hosipital"],),
                )
                curs.execute(
                    "delete from case_info_draft where save_user=%s",
                    (data["save_user"],),
                )
            return success({"id": res, "msg": "success"})
        except psycopg2.DatabaseError as error:
            print(sql)
            print(error)
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 获取
    if request.method == "GET":
        enum = Enum()
        enum.hosipital = enum.hosipitalList()
        # url携带参数
        hosipital = request.args.get("hosipital")
        save_user = request.args.get("username")
        limit, offset = handle_pagination(request)
        if not save_user:
            curs.execute(
                CASE_SEL_PAGE_KEYWORDS
                + " from case_info a,doctor b where a.is_del=False and b.phone_number=a.save_user and a.hosipital=%s order by save_time desc limit %s offset %s",
                (hosipital, limit, offset),
            )
            has_draft = False
        else:
            curs.execute(
                "select id from case_info_draft where save_user=%s", (save_user,)
            )
            draft = curs.fetchone()
            if draft:
                has_draft = True
                if offset == 0:
                    limit = limit - 1
                else:
                    offset = offset - 1
            else:
                has_draft = False
            curs.execute(
                CASE_SEL_PAGE_KEYWORDS
                + " from case_info a,doctor b where a.is_del=False and b.phone_number=a.save_user and a.hosipital=%s and a.save_user=%s order by save_time desc limit %s offset %s",
                (hosipital, save_user, limit, offset),
            )
        try:
            all = curs.fetchall()
            content = {}
            payload = []
            if has_draft and offset == 0:
                curs.execute(
                    CASE_SEL_KEYWORDS
                    + """from case_info_draft a,doctor b where b.phone_number=a.save_user"""
                )
                data = curs.fetchone()
                payload.append(
                    {
                        "id": data[47],
                        "name": data[0],
                        "phone_num1": data[1],
                        "phone_num2": data[2],
                        "sex": data[3],
                        "height": data[4],
                        "weight": data[5],
                        "id_card": data[6],
                        "province": data[7],
                        "city": data[8],
                        "district": data[9],
                        "town": data[10],
                        "street": data[11],
                        "detail_address": data[12],
                        "job": data[13],
                        "disease": data[14],
                        "health_care": data[15],
                        "surface_part": data[16],
                        "surface_area": data[17],
                        "surface_type": data[18],
                        "metabolic_ulcer": data[19],
                        "dm_time": data[20],
                        "dm_used": data[21],
                        "dm_course": data[22],
                        "df_time": data[23],
                        "df_cause": data[24],
                        "df_part": data[25],
                        "operation_part": data[26],
                        "amputation_part": data[27],
                        "smoke": data[28],
                        "drink_years": data[29],
                        "drinking": data[30],
                        "start_time": data[31],
                        "before_day": data[32],
                        "cure_way": data[33],
                        "pathogenic_bacteria": data[34],
                        "hosipital": enum.hosipital_name(data[35]),
                        "hosipital_address": data[36],
                        "doctor": data[37],
                        "department": data[38],
                        "in_day": data[39],
                        "cost": data[40],
                        "course": data[41],
                        "cure_outcome": data[42],
                        "operation_course": data[43],
                        # "file":data[44],
                        "save_time": data[45],
                        "save_user": data[46],
                        "birthday": data[48],
                        "case_source": data[49],
                        "edu": data[50],
                        "dc": data[51],
                        "wagner": data[52],
                        "year": data[53],
                        "bmi": data[54],
                        "hba1c": data[55],
                        "wbc": data[56],
                        "crp": data[57],
                        "procalcitonin": data[58],
                        "fpg": data[59],
                        "uric_acid": data[60],
                        "triglyceride": data[61],
                        "total_cholesterol": data[62],
                        "hdlc": data[63],
                        "ldlc": data[64],
                        "hemoglobin": data[65],
                        "alb": data[66],
                        "abi": data[67],
                        "cre": data[68],
                        "tg": data[69],
                        "platelet": data[70],
                        "neut": data[71],
                    }
                )
            for data in all:
                content = {
                    "id": data[47],
                    "name": data[0],
                    "phone_num1": data[1],
                    "phone_num2": data[2],
                    "sex": data[3],
                    "height": data[4],
                    "weight": data[5],
                    "id_card": data[6],
                    "province": data[7],
                    "city": data[8],
                    "district": data[9],
                    "town": data[10],
                    "street": data[11],
                    "detail_address": data[12],
                    "job": data[13],
                    "disease": data[14],
                    "health_care": data[15],
                    "surface_part": data[16],
                    "surface_area": data[17],
                    "surface_type": data[18],
                    "metabolic_ulcer": data[19],
                    "dm_time": data[20],
                    "dm_used": data[21],
                    "dm_course": data[22],
                    "df_time": data[23],
                    "df_cause": data[24],
                    "df_part": data[25],
                    "operation_part": data[26],
                    "amputation_part": data[27],
                    "smoke": data[28],
                    "drink_years": data[29],
                    "drinking": data[30],
                    "start_time": data[31],
                    "before_day": data[32],
                    "cure_way": data[33],
                    "pathogenic_bacteria": data[34],
                    "hosipital": enum.hosipital_name(data[35]),
                    "hosipital_address": data[36],
                    "doctor": data[37],
                    "department": data[38],
                    "in_day": data[39],
                    "cost": data[40],
                    "course": data[41],
                    "cure_outcome": data[42],
                    "operation_course": data[43],
                    # "file":data[44],
                    "save_time": data[45],
                    "save_user": data[46],
                    "birthday": data[48],
                    "case_source": data[49],
                    "edu": data[50],
                    "dc": data[51],
                    "wagner": data[52],
                    "year": data[53],
                    "bmi": data[54],
                    "hba1c": data[55],
                    "wbc": data[56],
                    "crp": data[57],
                    "procalcitonin": data[58],
                    "fpg": data[59],
                    "uric_acid": data[60],
                    "triglyceride": data[61],
                    "total_cholesterol": data[62],
                    "hdlc": data[63],
                    "ldlc": data[64],
                    "hemoglobin": data[65],
                    "alb": data[66],
                    "abi": data[67],
                    "cre": data[68],
                    "tg": data[69],
                    "platelet": data[70],
                    "neut": data[71],
                }
                payload.append(content)
            if has_draft:
                if all:
                    total = data[72] + 1
                else:
                    total = 1
            else:
                if all:
                    total = data[72]
                else:
                    total = 0
            return jsonify(
                {"status": 1, "has_draft": has_draft, "total": total, "resp": payload}
            )
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


@app.route("/caseinfo/<id>", methods=["PUT", "GET", "DELETE"])
@login_required(0)
def case_info(id):
    db = DB(ps_pool)
    # enum.city=enum.cityList()
    # enum.province=enum.provinceList()
    curs = db.start_db()
    # 获取
    if request.method == "GET":
        enum = Enum()
        enum.hosipital = enum.hosipitalList()
        is_draft = request.args.get("is_draft")
        if is_draft:
            sql = (
                CASE_SEL_KEYWORDS
                + """ from case_info_draft a,doctor b
        where a.id={} and b.phone_number=a.save_user;""".format(
                    id
                )
            )
        else:
            sql = (
                CASE_SEL_KEYWORDS
                + """ from case_info a,doctor b
        where a.id={} and a.is_del=False and b.phone_number=a.save_user;""".format(
                    id
                )
            )
        try:
            res = curs.execute(sql)
            data = curs.fetchall()[0]
            if not data:
                return success("")
            payload = {
                "id": data[47],
                "name": data[0],
                "phone_num1": data[1],
                "phone_num2": data[2],
                "sex": data[3],
                "height": data[4],
                "weight": data[5],
                "id_card": data[6],
                "province": data[7],
                "city": data[8],
                "district": data[9],
                "town": data[10],
                "street": data[11],
                "detail_address": data[12],
                "job": data[13],
                "disease": data[14],
                "health_care": data[15],
                "surface_part": data[16],
                "surface_area": data[17],
                "surface_type": data[18],
                "metabolic_ulcer": data[19],
                "dm_time": data[20],
                "dm_used": data[21],
                "dm_course": data[22],
                "df_time": data[23],
                "df_cause": data[24],
                "df_part": data[25],
                "operation_part": data[26],
                "amputation_part": data[27],
                "smoke": data[28],
                "drink_years": data[29],
                "drinking": data[30],
                "start_time": data[31],
                "before_day": data[32],
                "cure_way": data[33],
                "pathogenic_bacteria": data[34],
                "hosipital": enum.hosipital_name(data[35]),
                "hosipital_address": data[36],
                "doctor": data[37],
                "department": data[38],
                "in_day": data[39],
                "cost": data[40],
                "course": data[41],
                "cure_outcome": data[42],
                "operation_course": data[43],
                # "file":data[44],
                "save_time": data[45],
                "save_user": data[46],
                "birthday": data[48],
                "case_source": data[49],
                "edu": data[50],
                "dc": data[51],
                "wagner": data[52],
                "year": data[53],
                "bmi": data[54],
                "hba1c": data[55],
                "wbc": data[56],
                "crp": data[57],
                "procalcitonin": data[58],
                "fpg": data[59],
                "uric_acid": data[60],
                "triglyceride": data[61],
                "total_cholesterol": data[62],
                "hdlc": data[63],
                "ldlc": data[64],
                "hemoglobin": data[65],
                "alb": data[66],
                "abi": data[67],
                "cre": data[68],
                "tg": data[69],
                "platelet": data[70],
                "neut": data[71],
            }
            return success(payload)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 修改
    if request.method == "PUT":
        data = json.loads(request.get_data(as_text=True))
        data["id"] = id
        # data['province']=enum.provinceId(data['province'])
        # data['city']=enum.cityId(data['city'])
        # data['hosipital']=enum.hosipitalId(data['hosipital'])
        data["save_time"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        data["bmi"] = cal_bmi(data["height"], data["weight"])
        data["tg"] = -1
        sql = """update case_info set 
                name='{name}',phone_num2='{phone_num2}',sex='{sex}',height={height},weight={weight},id_card='{id_card}',
                province='{province}',city='{city}',district='{district}',town='{town}',street='{street}',detail_address='{detail_address}',
                job='{job}',disease='{disease}',health_care='{health_care}',surface_part='{surface_part}',surface_area='{surface_area}',
                surface_type='{surface_type}',metabolic_ulcer='{metabolic_ulcer}',dm_time='{dm_time}',dm_used='{dm_used}',
                dm_course='{dm_course}',df_time='{df_time}',df_cause='{df_cause}',df_part='{df_part}',operation_part='{operation_part}',
                amputation_part='{amputation_part}',smoke='{smoke}',drink_years='{drink_years}',drinking='{drinking}',
                start_time='{start_time}',before_day='{before_day}',cure_way='{cure_way}',pathogenic_bacteria='{pathogenic_bacteria}',
                hosipital={hosipital},doctor='{doctor}',department='{department}',
                in_day='{in_day}',cost={cost},course='{course}',cure_outcome='{cure_outcome}',operation_course='{operation_course}',
                file='{file}',save_time='{save_time}',save_user='{save_user}',case_source='{case_source}',birthday='{birthday}',edu='{edu}',
                dc='{dc}',wagner='{wagner}',year={year},bmi={bmi},hba1c={HbA10},wbc={wbc},crp={crp},procalcitonin={procalcitonin},fpg={fpg},
                uric_acid={uric_acid},triglyceride={triglyceride},total_cholesterol={total_cholesterol},hdlc={HDLC},ldlc={LDLC},
                hemoglobin={hemoglobin},alb={alb},abi={ABI},cre={cre},tg={tg},platelet={platelet},neut={neut} where id={id}
                returning id""".format(
            **data
        )
        try:
            curs.execute(sql)
            res = curs.fetchone()[0]
            return success(res)
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)
    # 删除
    if request.method == "DELETE":
        try:
            is_draft = request.args.get("is_draft")
            if is_draft:
                curs.execute("delete from case_info_draft where id=%s", (id,))
            else:
                curs.execute(
                    "update case_info set is_del=True where id=%s returning save_user, hosipital",
                    (id,),
                )
                res = curs.fetchone()
                curs.execute(
                    "update doctor set num_patients=num_patients-1 where phone_number=%s",
                    (res[0],),
                )
                curs.execute(
                    "update hosipital set num_cases=num_cases-1 where id=%s", (res[1],)
                )
            return success()
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


# 用户登录
@app.route("/user/login", methods=["POST"])
def login():
    if request.method == "POST":
        db = DB(ps_pool)
        curs = db.start_db()
        data = json.loads(request.get_data(as_text=True))
        site_info = request.args.to_dict()
        choice = site_info.get("choice")
        if not choice or choice == "0":
            sql = "select id, hosipital,is_new from admin where phone='{username}' and password='{password}' or account='{username}' and password='{password}'".format(
                **data
            )
        if choice == "1":
            sql = "select id, hosipital,is_new from doctor where phone_number='{username}' and password='{password}' and is_valid=True".format(
                **data
            )
        try:
            curs.execute(sql)
            res = curs.fetchone()
            if not res:
                return fail(status=0, error="用户名或密码错误！")
            session["username"] = data["username"]
            session["hosipital"] = res[1]
            is_new = res[2]
            curs.execute("select name from hosipital where id=%s", (res[1],))
            res = curs.fetchone()
            session["hosipital_name"] = res[0]
            if not choice or choice == "0":
                session["role"] = 10
            else:
                session["role"] = 1
            return jsonify({"status": 1, "is_new": is_new, "resp": "登录成功"})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": str(error)})
        finally:
            db.finish_db(curs)


# 注销用户
@app.route("/logout", methods=["GET"])
def logout():
    session.clear()
    return jsonify(msg="退出成功")


@app.route("/password", methods=["POST"])
@login_required(1)
def modify_user():
    data = json.loads(request.get_data(as_text=True))
    # site_info = request.args.to_dict()
    db = DB(ps_pool)
    username = session.get("username")
    curs = db.start_db()
    role = session.get("role")

    if int(role) == 10:
        sql = "select password from admin where phone='{username}'".format(
            username=username
        )
    else:
        sql = "select password from doctor where phone_number='{username}'".format(
            username=username
        )
    curs.execute(sql)
    res = curs.fetchall()
    if data["old_pwd"] != res[0][0]:
        return fail(error="原密码不正确")
    if int(role) == 10:
        upd_sql = "update admin set password='{new_pwd}',is_new=false where phone='{username}'".format(
            username=username, new_pwd=data["new_pwd"]
        )
    else:
        upd_sql = "update doctor set password='{new_pwd}',is_new=false where phone_number='{username}'".format(
            username=username, new_pwd=data["new_pwd"]
        )
    curs.execute(upd_sql)
    db.finish_db(curs)
    return success("修改成功！")


@app.route("/reset/<phone_number>", methods=["POST"])
def reset_password(phone_number):
    token = request.json["token"]
    role = request.json["role"]
    if token != app.token:
        return jsonify({"status": 0, "resp": "token不正确！"})
    r = redis.Redis(connection_pool=redis_pool)
    status = r.get(phone_number + "verify_code")
    if status:
        db = DB(ps_pool)
        try:
            curs = db.start_db()
            if int(role) == 10:
                curs.execute(
                    "update admin set password=%s where phone=%s",
                    ("12345678", phone_number),
                )
            else:
                curs.execute(
                    "update doctor set password=%s where phone_number=%s",
                    ("123456", phone_number),
                )
            return jsonify({"status": 1, "resp": 1})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": 0})
        finally:
            db.finish_db(curs)
    else:
        return jsonify({"status": 1, "resp": -1}), 403


@app.route("/reset", methods=["POST"])
def reset_check_code():
    token = request.json["token"]
    if token != app.token:
        return jsonify({"status": 0, "resp": "token不正确！"})
    phone_number = request.json["phone_number"]
    role = request.json.get("role")
    code = request.json.get("code")
    if not code:
        db = DB(ps_pool)
        try:
            curs = db.start_db()
            if int(role) == 10:
                curs.execute("select id from admin where phone=%s", (phone_number,))
            else:
                curs.execute(
                    "select id from doctor where phone_number=%s", (phone_number,)
                )
            if curs.fetchone():
                status = SMSVerfication.send_sms(phone_number, redis_pool)
                if status:
                    return jsonify({"status": 1, "resp": "发送成功"})
                else:
                    return jsonify({"status": -1, "resp": "短信服务出错，暂不可用，请联系管理员处理！"})
            else:
                return jsonify({"status": -1, "resp": "该用户还未注册！请联系管理员注册新用户！"})
        except psycopg2.DatabaseError as error:
            return jsonify({"status": -1, "resp": "应用出错，请联系管理员！"})
        finally:
            db.finish_db(curs)
    else:
        status = SMSVerfication.verify_code(phone_number, code, redis_pool)
        if status >= 0:
            return jsonify({"status": status, "resp": "OK"})
        else:
            return jsonify({"status": -1, "resp": "还未发送验证短信！"})


@app.route("/session", methods=["GET"])
def ret():
    name = session.get("username")
    role = session.get("role")
    hosipital = session.get("hosipital")
    return success(
        {
            "name": name,
            "role": role,
            "hosipital": hosipital,
            "hosipital_name": session.get("hosipital_name"),
        }
    )


@app.route("/checkid", methods=["GET"])
@login_required(1)
def checkid():
    r = redis.Redis(connection_pool=redis_pool)
    id = request.args.get("id")
    if not id:
        return jsonify({"status": 0, "resp": "无id，错误参数"})
    db = DB(ps_pool)
    try:
        curs = db.start_db()
        return jsonify({"status": 1, "resp": {"code": 0}})
        return jsonify({"status": 1, "resp": {"code": 1, "content": "该人员已经录入过"}})
    except psycopg2.DatabaseError as error:
        return jsonify({"status": -1, "resp": "应用出错，请联系管理员！"})
    finally:
        db.finish_db(curs)


if __name__ == "__main__":
    # app.run()
    http_server = WSGIServer(("127.0.0.1", 6000), app)
    http_server.serve_forever()
