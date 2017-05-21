package online.pins24.remotestartengine;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsFragment extends BaseFragment
{
    SeekBar seekBar;
    SharedPreferences sharedPref;
    TextView minV, maxV, currV;
    Button bSetTemperStart, bResetTemperStart;
    View view;

    private int currTempValue = 0;
    private final String SHAREDPREF = "SharedPref";
    private final String TEMPVALSHAREDPREF = "TempValSharedPref";
    private final String PHONESHAREDPREF = "PhoneSharedPref";
    private final String CELSIUS = "\u2103";
    private static int stepSeek = 1;
    private int maxSeek = 50;
    private int minSeek = -50;
    private String currentPhone;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //ищем наши вьюхи на активити
        findViews();
        setDefaultSettings();
        fillData();
    }

    //region findViews() Поиск вьюх определенных в R.id
    private void findViews()
    {
        View rootView = getView();
        seekBar = (SeekBar) rootView.findViewById(R.id.seekBarTemp);
        minV = (TextView) rootView.findViewById(R.id.tvMinVal);
        maxV = (TextView) rootView.findViewById(R.id.tvMaxVal);
        currV = (TextView) rootView.findViewById(R.id.tvCurrValTemp);
        bSetTemperStart = (Button) rootView.findViewById(R.id.bSetTemper);
        bResetTemperStart = (Button) rootView.findViewById(R.id.bResetTemper);
    }
    //endregion

    private void setDefaultSettings()
    {
        seekBar.setMax((maxSeek - minSeek) / stepSeek);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        minV.setText(Integer.toString(minSeek) + CELSIUS);
        maxV.setText(Integer.toString(maxSeek) + CELSIUS);
        currV.setText(Integer.toString(currTempValue));
    }

    //region fillData() Загрузка данных при старте приложения
    private void fillData()
    {
        Context context = getContext();
        Toast.makeText(context, "Загрузка...", Toast.LENGTH_SHORT).show();
        //Используем созданный файл данных SharedPreferences:
        sharedPref = context.getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE);
        currentPhone = sharedPref.getString(PHONESHAREDPREF, null);
        currTempValue = Integer.parseInt(sharedPref.getString(TEMPVALSHAREDPREF, "0"));
        currV.setText(sharedPref.getString(TEMPVALSHAREDPREF, "0") + CELSIUS);
        seekBar.setProgress(currTempValue + maxSeek);
    }
    //endregion

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b)
                {
                    currTempValue = minSeek + (i * stepSeek);
                    currV.setText(Integer.toString(currTempValue) + CELSIUS);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar)
                {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar)
                {

                }
            };

    //region SaveSharedPref() Читаем сохраненные настройки
    private void saveSharedPref()
    {
        Context context = getContext();
        Toast.makeText(getContext(), "Сохраняем...", Toast.LENGTH_SHORT).show();
        //Создаем объект Editor для создания пар имя-значение:
        sharedPref = context.getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE);
        //Создаем объект Editor для создания пар имя-значение:
        SharedPreferences.Editor shpEditor = sharedPref.edit();
        shpEditor.putString(TEMPVALSHAREDPREF, Integer.toString(currTempValue));
        shpEditor.commit();
    }
    //endregion

    private void askForSave()
    {
        //Спрашиваем надо ли нам это
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Настройки...");
        alertDialog.setMessage("Вы уверены, что выбранные настройки уже сохранены на контроллере автомобиля?\n" +
                               "Если нет, то нажмите кнопку Применить!");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //пишем если надо
                saveSharedPref();
                // ? finish();
            }
        });
        //endregion

        //region NO CLICK
        alertDialog.setNegativeButton("НЕТ", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                // ? finish();
            }
        });
        //endregion
        alertDialog.show();
    }

    //region startButtonClick Обработка нажатия кнопки запись температуры на устройство
    public void startTemperButtonClick(View view)
    {
        //Спрашиваем надо ли нам это
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Настройки...");
        alertDialog.setMessage("Уверены?");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //пишем если надо
                doStartTemperEngine();
            }
        });
        //endregion

        //region NO CLICK
        alertDialog.setNegativeButton("НЕТ", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        //endregion
        alertDialog.show();
    }
    //endregion

    //region doStartEngine() Выполняем запись
    private void doStartTemperEngine()
    {
        if (TextUtils.isEmpty(currentPhone))
        {
            showToast(view, "Мобильный номер неопределен!");
            return;
        }
        saveSharedPref();
        sendSms(currentPhone, "temp;" + Integer.toString(currTempValue));
        showToast(view, "Настройки контроллера, для запуска двигателя по температуре, применены!");
    }
    //endregion

    private void showToast(View view, String str)
    {
        //создаем и отображаем текстовое уведомление
        Toast toast = Toast.makeText(getContext(), str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFFCB3E3E);
        gd.setCornerRadius(10);

        LinearLayout v = (LinearLayout)toast.getView();

        TextView toastView = (TextView) v.getChildAt(0);
        toastView.setPadding(10, 10, 10, 10);
        toastView.setTextSize(14);

        if(Build.VERSION.SDK_INT >= 16)
            toastView.setBackground(gd);
        else toastView.setBackgroundDrawable(gd);
        toast.show();
    }

    //region SendSms Функция отправки смс
    public void sendSms(String number, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, null, null);
    }
    //endregion

    //region stopButtonClick Обработка нажатия кнопки сброса запуска по температуре
    public void stopTemperButtonClick(View view)
    {
        //Спросили хотим ли застопить принудительно
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Настройки...");
        alertDialog.setMessage("Уверены?");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //Если захотели все же
                doStopTemperEngine();
            }
        });
        //endregion

        //region NO CLICK
        alertDialog.setNegativeButton("НЕТ", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        //endregion
        alertDialog.show();
    }
    //endregion

    //region doStopEngine() Выполняем сброс темературы
    private void doStopTemperEngine()
    {
        if (TextUtils.isEmpty(currentPhone))
        {
            showToast(view, "Мобильный номер неопределен!");
            return;
        }
        currTempValue = 0;
        currV.setText("0" + CELSIUS);
        seekBar.setProgress(currTempValue + maxSeek);
        saveSharedPref();
        sendSms(currentPhone, "temp;" + Integer.toString(currTempValue));
        showToast(view, "Запуск двигателя по температуре деактивирован!");
    }
    //endregion

//    //region onPause() Чего делаем если свернули приложуху - сохраняемся же!
//    @Override
//    protected void onPause()
//    {
//        super.onPause();
//        saveSharedPref();
//    }
//    //endregion
}