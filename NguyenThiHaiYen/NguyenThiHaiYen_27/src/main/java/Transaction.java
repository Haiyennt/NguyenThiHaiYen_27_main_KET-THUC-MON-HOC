import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
public class Transaction {


    public String transactionId;
    public PublicKey sender; //Biến lưu Khóa công khai (địa chỉ) của người gửi
    public PublicKey reciepient; //Biến lưu Khóa công khai (địa chỉ) của người nhận.
    public float value; //Biến lưu số tiền.
    public byte[] signature; //Biến lưu Chữ ký số.

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; //Biến đếm số lượng giao dịch tạo ra

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#Chu ky giao dich xac nhan khong dung!");
            return false;
        }

        //Thu thập các đầu vào cùa giao dịch chưa được thực thi
        for(TransactionInput i : inputs) {
            i.UTXO = NTHY_Blockchain.UTXOs.get(i.transactionOutputId);
        }

        //Kiểm tra giao dịch hợp lệ
        if(getInputsValue() < NTHY_Blockchain.minimumTransaction) {
            System.out.println("Dau vao giao dich qua be: " + getInputsValue());
            System.out.println("Vui long nhap so luong giao dich lon hon " + NTHY_Blockchain.minimumTransaction);
            return false;
        }

        //Sinh các đầu ra của giao dịch
        float leftOver = getInputsValue() - value; //Tính giá trị còn lại sau khi giao dịch
        transactionId = calulateHash();
        outputs.add(new TransactionOutput( this.reciepient, value,transactionId)); //Gửi giá trị  đến người nhận
        outputs.add(new TransactionOutput( this.sender, leftOver,transactionId)); //Gửi giá trị còn lại về người gửi

        //Đưa giao dịch vào UTXO
        for(TransactionOutput o : outputs) {
            NTHY_Blockchain.UTXOs.put(o.id , o);
        }

        //Xóa các đầu vào giao dịch ra khỏi danh sách UTXO sau khi đã thực hiện
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //Nếu danh sách UTXO rỗng thì bỏ qua
            NTHY_Blockchain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //Nếu danh sách UTXO rỗng thì bỏ qua
            total += i.UTXO.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        signature = StringUtil.applyECDSASig(privateKey,data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value)	;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

    private String calulateHash() {
        sequence++; //Tăng biến đếm giao dịch lên 1 đơn vị để tránh hai giao dịch có mã băm giống nhau
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }
}
