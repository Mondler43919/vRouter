use jni::sys::{jboolean, jobject};
use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JValue};


use curve25519_dalek::ristretto::{CompressedRistretto, RistrettoPoint};
use curve25519_dalek::scalar::Scalar;
use curve25519_dalek::constants::RISTRETTO_BASEPOINT_POINT;
use sha2::{Sha512, Digest};

use rand::rngs::OsRng;
use rand::RngCore;
use rand_chacha::ChaCha20Rng;
use rand_chacha::rand_core::SeedableRng;
use std::convert::TryInto;

/// 生成密钥对
#[no_mangle]
pub extern "system" fn Java_vRouter_VRFElection_generateKeyPair(
    mut env: JNIEnv,
    _class: JClass,
) -> jobject {
    let mut rng = OsRng;
    let mut private_key = [0u8; 32];
    rng.fill_bytes(&mut private_key);
    
    // 从私钥生成公钥
    let scalar = Scalar::from_bytes_mod_order(private_key);
    let public_key = (scalar * RISTRETTO_BASEPOINT_POINT).compress();

    // 创建Java字节数组
    let priv_arr = env.byte_array_from_slice(&private_key).unwrap();
    let pub_arr = env.byte_array_from_slice(public_key.as_bytes()).unwrap();

    // 创建Java KeyPair 对象
    let class = env.find_class("vRouter/VRFElection$KeyPair").unwrap();
    let obj = env.new_object(
        class,
        "([B[B)V",
        &[
            jni::objects::JValue::Object(&priv_arr.into()),
            jni::objects::JValue::Object(&pub_arr.into()),
        ],
    )
    .unwrap();

    obj.into_raw()
}

/// 任意哈希输入转换到椭圆曲线上
fn hash_to_point(input: &[u8]) -> RistrettoPoint {
    let hash = Sha512::digest(input);
    RistrettoPoint::from_uniform_bytes(&hash[0..64].try_into().unwrap())
}

/// VRF生成
#[no_mangle]
pub extern "system" fn Java_vRouter_VRFElection_generateVRF(
    mut env: JNIEnv,  // 添加 mut 修饰符
    _class: JClass,
    priv_key: JByteArray,
    input: JByteArray,
) -> jobject {
    let priv_bytes = env.convert_byte_array(priv_key).unwrap();
    let input_bytes = env.convert_byte_array(input).unwrap();

    // 计算H = Hash(input), output = privateKey * H
    let scalar = Scalar::from_bytes_mod_order(priv_bytes.try_into().unwrap());
    let H = hash_to_point(&input_bytes);
    let gamma = scalar * H;

    let mut rng = ChaCha20Rng::from_rng(OsRng).unwrap();

    // 生成随机标量 k
    let mut k_bytes = [0u8; 32];
    rng.fill_bytes(&mut k_bytes);
    let k = Scalar::from_bytes_mod_order(k_bytes);

    // U = k * G, V = k * H
    let U = k * RISTRETTO_BASEPOINT_POINT;
    let V = k * H;

    // 计算 c = hash(U||V)
    let mut hasher = Sha512::new();
    hasher.update(U.compress().as_bytes());
    hasher.update(V.compress().as_bytes());
    let c_hash = hasher.finalize();
    let c = Scalar::from_bytes_mod_order(c_hash[0..32].try_into().unwrap());

    // 计算s = k + c * scalar
    let s = k + c * scalar;

    // 输出 VRF 值
    let output_hash = Sha512::digest(gamma.compress().as_bytes());
    let vrf_output = &output_hash[0..32];

    // 拼接 proof = gamma(32) || c(32) || s(32)
    let proof = [
        gamma.compress().as_bytes(),
        c.to_bytes().as_ref(),
        s.to_bytes().as_ref(),
    ]
    .concat();

    // 创建Java字节数组
    let output_arr = env.byte_array_from_slice(vrf_output).unwrap();
    let proof_arr = env.byte_array_from_slice(&proof).unwrap();

    // 创建Java对象
    let class = env.find_class("vRouter/VRFElection$VRFOutput").unwrap();
    let obj = env.new_object(
        class,
        "([B[B)V",
        &[
            JValue::Object(&output_arr.into()),
            JValue::Object(&proof_arr.into()),
        ],
    )
    .unwrap();

    // 直接返回 jobject
    obj.into_raw()
}

/// VRF验证
#[no_mangle]
pub extern "system" fn Java_vRouter_VRFElection_verifyVRFProof(
    mut env: JNIEnv,  // 添加 mut 修饰符
    _class: JClass,
    pub_key: JByteArray,
    input: JByteArray,
    proof: JByteArray,
) -> jboolean {
    let pub_bytes = env.convert_byte_array(pub_key).unwrap();
    let input_bytes = env.convert_byte_array(input).unwrap();
    let proof_bytes = env.convert_byte_array(proof).unwrap();

    if proof_bytes.len() != 96 {
        return 0;
    }
    // 解析证明
    let gamma = match CompressedRistretto::from_slice(&proof_bytes[0..32]) {
        Ok(compressed) => match compressed.decompress() {
            Some(p) => p,
            None => return 0,
        },
        Err(_) => return 0,
    };

    let c = Scalar::from_bytes_mod_order(proof_bytes[32..64].try_into().unwrap());
    let s = Scalar::from_bytes_mod_order(proof_bytes[64..96].try_into().unwrap());
    // 重新计算H，Q
    let H = hash_to_point(&input_bytes);
    let Q = match CompressedRistretto::from_slice(&pub_bytes) {
        Ok(compressed) => match compressed.decompress() {
            Some(p) => p,
            None => return 0,
        },
        Err(_) => return 0,
    };
    // 计算U'， V'
    let U_prime = s * RISTRETTO_BASEPOINT_POINT - c * Q;
    let V_prime = s * H - c * gamma;

    let mut hasher = Sha512::new();
    hasher.update(U_prime.compress().as_bytes());
    hasher.update(V_prime.compress().as_bytes());
    let c_prime_hash = hasher.finalize();
    let c_prime = Scalar::from_bytes_mod_order(c_prime_hash[0..32].try_into().unwrap());

    if c == c_prime { 1 } else { 0 }
}