function price2(N_EV)

%Load Aggregator data
dataName='ADMM_matlab/Aggregator/aggregator.mat';
agr_params= load(dataName); % data required by the agregator

N=N_EV+1;                       % Number of agents
deltaT=15*60;                   % Time slot duration [sec]
T= 24*3600/deltaT ;             % Number of time slots

price = agr_params.price;       %Price
p=repmat(price,4,1);
p=p(:) / (3600*1000) * deltaT;  % scaling of price in EUR/kW
agr_params.p=p;

agr_params.re=   60*ones(T,1);  % maximal aviliable load 1GW 
agr_params.xamin=-100e3*ones(T,1);

x_max=4;                                 % Max charging power for greedy need to add some 1e-3 or someting for it to be feasible
x_min=-4;                                % Min charging power
rho =0.01;
alpha=0.05/3600 * 15*60  ;
gamma =1;
xmean=zeros(T,1);       % Avarage of all agent profiles 96*1
u = zeros(T,1);

x = zeros(T,N);
xold= x;


name=['ADMM_matlab/EVs/home/1.mat'];
data=load(name) ;

%assign data
A=data.A;
R=data.R;
d=data.d;
B=data.B;
Smax = data.S_max;
Smin = data.S_min;

%Contraints
Aineq=[B;-B];
bineq=[Smax+1e-4; -(Smin-1e-4)];
lb=d'*x_min;
ub=d'*x_max;
Aeq=A ;
beq=R';

%CVX Implementation
% cvx_begin
%         variable x_i(T)
%         
%         %minimize( (gamma*alpha*sum(x_i.*x_i)) + (rho/2 * norm(x_i - xold(:,1) + xmean + u))  )
%         minimize( (gamma*alpha*sum(x_i.*x_i)) + (rho/2 * sum((x_i - xold(:,1) + xmean + u).*(x_i - xold(:,1) + xmean + u)))  )
%         
%         subject to
%             Aineq * x_i <= bineq
%             Aeq * x_i == beq 
%             lb <= x_i <= ub
% cvx_end
% 
% disp(x_i)
% disp(sum(x_i))


%YALMIP IMPLEMENTATION
x_i= sdpvar(T,1,'full');
   
Cost= (gamma*alpha*sum(x_i.*x_i)) + (rho/2 * sum((x_i - xold(:,1) + xmean + u).*(x_i - xold(:,1) + xmean + u)));

Constraints=[Aineq * x_i <= bineq,Aeq * x_i == beq,lb <= x_i <= ub];
ops = sdpsettings('verbose',0);
savecplexlp(Constraints, Cost, 'myEVCplex.mod');
sol=solvesdp(Constraints,Cost,ops);
if sol.problem == 0
    x = double(x_i);
    disp(x)
end


end


