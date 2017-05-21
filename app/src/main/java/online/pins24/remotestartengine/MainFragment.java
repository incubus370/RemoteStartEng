package online.pins24.remotestartengine;

import android.Manifest;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

public class MainFragment extends BaseFragment
{
    //region КОНСТАНТЫ
    private final String SHAREDPREF = "SharedPref";
    private final String PHONESHAREDPREF = "PhoneSharedPref";
    private final String TEMPVALSHAREDPREF = "TempValSharedPref";
    private final String RESET = "reset";
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int SECONDS_TO_COUNTDOWN = 900;
    private final String CELSIUS = "\u2103";
    //endregion
    //region ОБЪЯВЛЯЕМ ОБЪЕКТЫ
    SharedPreferences sharedPref;
    private Button bStartEngine, bStopEngine, bResetDevice, bActivateDevice;
    private EditText etPhone;
    private TextView tvTimer, tvTempNotifIsActive;
    private LinearLayout imgLayout;
    Context appContext;
    private CountDownTimer timer;
    private MediaPlayer mp;
    //endregion
    //region ПЕРЕМЕННЫЕ
    private String currentPhone;
    private boolean startFlag = false;
    private String currTempStringValue = null;
    //endregion

    //region onCreate Конструктор мать его
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //получаем контекст приложения - ссылку на сам объект приложения (читай что такое контекст)
        appContext = getContext().getApplicationContext();
        //ищем наши вьюхи на активити
        findViews();
        //загрузка данных в переменные
        fillData();
    }
    //endregion

    //region findViews() Поиск вьюх определенных в R.id
    private void findViews()
    {
        View rootView = getView();
        bActivateDevice = (Button) rootView.findViewById(R.id.bActivateDevice);
        bResetDevice = (Button) rootView.findViewById(R.id.bResetDevice);
        bStartEngine = (Button) rootView.findViewById(R.id.bStartEngine);
        bStopEngine = (Button) rootView.findViewById(R.id.bStopEngine);
        etPhone = (EditText) rootView.findViewById(R.id.etPhone);
        tvTimer = (TextView) rootView.findViewById(R.id.tvTimer);
        tvTempNotifIsActive = (TextView) rootView.findViewById(R.id.tvTempNotif);
        imgLayout = (LinearLayout) rootView.findViewById(R.id.imageLayout);

        bStartEngine.setTextColor(Color.WHITE);
        bStopEngine.setTextColor(Color.WHITE);
        bActivateDevice.setTextColor(Color.WHITE);
        bResetDevice.setTextColor(Color.WHITE);
    }
    //endregion

    //region fillData() Загрузка данных при старте приложения
    private void fillData()
    {
        Context context = getContext();
        Toast.makeText(context, "Загрузка...", Toast.LENGTH_SHORT).show();
        //Используем созданный файл данных SharedPreferences:
        sharedPref = context.getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE);
        currentPhone = sharedPref.getString(PHONESHAREDPREF, null);
        currTempStringValue = sharedPref.getString(TEMPVALSHAREDPREF, null);
        etPhone.setText(currentPhone);

        //устанавливаем видимость объектов на экране
        setViewStates(true);

        if (!TextUtils.isEmpty(currentPhone))
        {
            //смена изображения машины на экране
            imgChangeCar(R.drawable.car_waiting);
            setViewStates(false);
        }

        if(TextUtils.isEmpty(currTempStringValue) || TextUtils.equals(currTempStringValue, "0"))
        {
            tvTempNotifIsActive.setVisibility(View.GONE);
        }
        else
        {
            String res = getString(R.string.temp_notification) + " " + currTempStringValue + CELSIUS;
            tvTempNotifIsActive.setVisibility(View.VISIBLE);
            tvTempNotifIsActive.setText(res);
        }
    }
    //endregion

    //region setViewStates Видимость объектов на экране
    private void setViewStates(Boolean state)
    {
        etPhone.setEnabled(state);
        bActivateDevice.setEnabled(state);
    }
    //endregion

    //region SaveSharedPref() Читаем сохраненные настройки
    private void saveSharedPref()
    {
        Context context = getContext();
        Toast.makeText(context, "Сохраняем...", Toast.LENGTH_SHORT).show();
        //Создаем объект Editor для создания пар имя-значение:
        sharedPref = context.getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE);
        //Создаем объект Editor для создания пар имя-значение:
        SharedPreferences.Editor shpEditor = sharedPref.edit();
        currentPhone = etPhone.getText().toString();
        shpEditor.putString(PHONESHAREDPREF, currentPhone);
        shpEditor.commit();
    }
    //endregion

    //region activateButtonClick Кнопка активации устройства
    public void activateButtonClick(View view)
    {
        //Сохранили перед активацией настройки приложухи в память
        saveSharedPref();
        //функция звонилка
        generalCallFunc();
    }
    //endregion

    //region generalCallFunc() Главная функция звонилка
    private void generalCallFunc()
    {
        currentPhone = etPhone.getText().toString();
        if (TextUtils.isEmpty(currentPhone))
        {
            etPhone.setError("Куда звоним?");
            return;
        }
        //куда именно звоним
        callToDevice(currentPhone);
    }
    //endregion

    //region callToDevice Куда именно звоним
    private void callToDevice(String phoneNum)
    {
        try
        {
            //Класс наблюдатель менеджера звонков
            PhoneCallListener phoneListener = new PhoneCallListener();
            //Сам менеджер звонков устройства - получаем сервис системный
            TelephonyManager telephonyManager = (TelephonyManager)appContext.getSystemService(Context
                    .TELEPHONY_SERVICE);
            //теперь определяем что нам надо делать, а именно отслеживаем само состояние подключения если у нас идет активный вызов
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

            //создаем интент звонилку
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            //парсим куда звоним и передаем на исполнение в интент
            callIntent.setData(Uri.parse("tel:" + phoneNum));
            //проверяем права приложения на звонок
            if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            //стартуем звонилку
            startActivity(callIntent);
        }
        catch (ActivityNotFoundException e)
        {
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    //endregion

    //region PhoneCallListener Наблюдатель активного звонка
    private class PhoneCallListener extends PhoneStateListener
    {
        private boolean isPhoneCalling = false;
        //тут остлеживаем текущее состояние активного вызова и его изменение
        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            //если сейчас идет звонок исходящий или входящий пофиг
            if (TelephonyManager.CALL_STATE_RINGING == state)
            {
                // phone ringing
            }
            //если положили трубу или скинули
            else if (TelephonyManager.CALL_STATE_OFFHOOK == state)
            {
                isPhoneCalling = true;
            }
            //если звонилка завершила свою работу
            else if (TelephonyManager.CALL_STATE_IDLE == state)
            {
                // run when class initial and phone call ended, need detect flag
                // from CALL_STATE_OFFHOOK
                if (isPhoneCalling)
                {
                    //тут смотрим наше действие - либо мы стартуем двигло
                    if (startFlag)
                    {
                        //тогда запускаем таймер прогрева и возвращаем наш экран приложухи обратно со всеми установками
                        runTimerAndReturnActivity();
                    }
                    //либо его глушим
                    else
                    {
                        //тогда стопим таймер и выставляем все вьюхи на экране согласно функционалу стоп
                        stopTimerAndReturnActivity();
                    }
                    isPhoneCalling = false;
                }
            }
        }
    }
    //endregion

    //region runTimerAndReturnActivity() Стартуем таймер прогрева и запускаем приложуние обратно после звонилки
    private void runTimerAndReturnActivity()
    {
        // restart app
        //получили интент нашей запущенной прложухи
        Intent i = appContext.getPackageManager().getLaunchIntentForPackage(appContext
                .getPackageName());
        //добавляем флаг что мы его возобновляем а не запускаем с нуля сброшенным
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //ну и стартуем приложуху
        startActivity(i);

        //меняем картинку что двигло начало греться
        imgChangeCar(R.drawable.car_started);
        //отрисовали на экране кнопки как надо - выключили чтобы лишнего не натыкать
        bResetDevice.setEnabled(false);
        bStartEngine.setEnabled(false);
        try
        {
            //пытаемся показать на экране таймер в миллисекундах
            showTimer(SECONDS_TO_COUNTDOWN * MILLIS_PER_SECOND);
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    //endregion

    //region stopTimerAndReturnActivity() Стоп таймера
    private void stopTimerAndReturnActivity()
    {
        // если таймер запущен то стопим его
        if(timer != null)
        {
            timer.cancel();
        }

        //опять же возобновляем приложуху а не запускаем ее заново
        Intent i = appContext.getPackageManager().getLaunchIntentForPackage(appContext
                .getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);

        //возвращаем начальные установки
        tvTimer.setText(getString(R.string.timerDefault));
        imgChangeCar(R.drawable.car_waiting);
        bResetDevice.setEnabled(true);
        bStartEngine.setEnabled(true);
        fillData();
    }
    //endregion

    //region showTimer Счетчик и отображение таймера на экране
    long durationSeconds = 0;
    private void showTimer(int countdownMillis)
    {
        //проверили что таймера нет
        if(timer != null)
        {
            timer.cancel();
        }
        //обратный отсчет
        timer = new CountDownTimer(countdownMillis, MILLIS_PER_SECOND)
        {
            //тикаем секунды и отрисовываем на экране в нужном формате
            @Override
            public void onTick(long millisUntilFinished)
            {
                durationSeconds = millisUntilFinished / MILLIS_PER_SECOND;
                tvTimer.setText(String.format("%02d:%02d:%02d",
                        durationSeconds / 3600,
                        (durationSeconds % 3600) / 60,
                        (durationSeconds % 60)));
                //когда осталось 7 секунд до конца играем мелодию будильник что двигло прогрето
                if (durationSeconds == 7)
                {
                    playEngineReady();
                }
            }
            //по завершении таймера возвращаем все на свои места
            @Override
            public void onFinish()
            {
                tvTimer.setText(getString(R.string.timerDefault));
                imgChangeCar(R.drawable.car_waiting);
                bResetDevice.setEnabled(true);
                bStartEngine.setEnabled(true);
            }
        }.start();
    }
    //endregion

    //region resetButtonClick Обработка нажатия кнопки сброса устройтва
    public void resetButtonClick(View view)
    {
        currentPhone = etPhone.getText().toString();

        //Задаем вопрос стоит ли нам сбрасывать
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Сброс устройства...");
        alertDialog.setMessage("Вы уверены?");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //Если ДА то сбрасываем устройство
                doReset(currentPhone);
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

    //region doReset Сброс устройства
    private void doReset(String number)
    {
        if (TextUtils.isEmpty(number))
        {
            etPhone.setError("Что сбрасываем?");
            return;
        }
        //Посылаем команду на сброс устройству
        sendSms(number, RESET);
        //вертаем все на место
        setViewStates(true);
        etPhone.setText(null);
        //сохраняемся
        saveSharedPref();
        //картинку в начальное состояние предактивации
        imgChangeCar(R.drawable.car_background);
    }
    //endregion

    //region SendSms Функция отправки смс
    public void sendSms(String number, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, message, null, null);
    }
    //endregion

    //region startButtonClick Обработка нажатия кнопки Старт
    public void startButtonClick(View view)
    {
        //Спрашиваем надо ли нам это
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Старт авто...");
        alertDialog.setMessage("Заводим?");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //Стартуем двигло если надо
                doStartEngine();
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

    //region doStartEngine() Выполняем старт двигла если захотели все таки
    private void doStartEngine()
    {
        startFlag = true;
        //Вызываем звонилку
        generalCallFunc();
    }
    //endregion

    //region imgChangeCar Функция смены изображений - старт или стоп или сброс
    private void imgChangeCar(int idDrawable)
    {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
        {
            imgLayout.setBackgroundDrawable(getResources().getDrawable(idDrawable));
        }
        else
        {
            imgLayout.setBackground(getResources().getDrawable(idDrawable));
        }
    }
    //endregion

    //region playEngineReady() Будильник для таймера
    private void playEngineReady()
    {
        //Выбрали мелодию и проиграли ее
        mp = MediaPlayer.create(appContext, R.raw.ready_long);
        try
        {
            if (mp.isPlaying())
            {
                mp.stop();
                mp.release();
                mp = MediaPlayer.create(appContext, R.raw.ready_long);
            }
            mp.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    //endregion

    //region stopButtonClick Обработка нажатия кнопки Стоп
    public void stopButtonClick(View view)
    {
        //Спросили хотим ли застопить принудительно
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("Стоп машина...");
        alertDialog.setMessage("Глушим?");

        //region YES CLICK
        alertDialog.setPositiveButton("ДА", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                //Если захотели все же то застопили
                doStopEngine();
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

    //region doStopEngine() Выполняем стоп двигла по звонку
    private void doStopEngine()
    {
        startFlag = false;
        generalCallFunc();
    }
    //endregion

    @Override
    public void onResume()
    {
        super.onResume();
        fillData();
    }

    //region onPause() Чего делаем если свернули приложуху - сохраняемся же!
    @Override
    public void onPause()
    {
        super.onPause();
        saveSharedPref();
        //и еще убрали фокус с номеранаберателя чтобы глаза клава не мозолила
        etPhone.clearFocus();
    }
    //endregion
}